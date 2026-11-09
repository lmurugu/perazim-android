'use strict';

const express = require('express');
const { requireAuth } = require('../middleware/auth');
const authRouter = require('./auth');
const { checkClaimRateLimit } = require('../phase-b/claimRateLimit');
const { matchesMembershipIdentity, normalizeName, readMembershipAccount } = require('../phase-b/membershipIdentity');
const {
  PhaseBError, asError, newId, phaseBTransaction, requirePhaseB,
  listEffectivePermissions, userPermission, ensureSelfServiceGrants
} = require('../phase-b/security');

const router = express.Router();

function validUuid(value) {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

async function currentVerification(client, userId, now, lock = false) {
  await client.query(
    `INSERT INTO membership_verifications (user_id,status,revision,updated_at)
     VALUES ($1,'REGISTERED',1,$2) ON CONFLICT (user_id) DO NOTHING`, [userId, now]
  );
  const result = await client.query(`SELECT status,revision,updated_at FROM membership_verifications WHERE user_id=$1${lock ? ' FOR UPDATE' : ''}`, [userId]);
  return result.rows[0];
}

async function classifyClaim(userId, claimId) {
  try {
    await phaseBTransaction(async client => {
      const claim = await client.query('SELECT id,user_id,user_name,user_phone,status FROM membership_claims WHERE id=$1 AND user_id=$2 FOR UPDATE', [claimId, userId]);
      if (!claim.rows.length || claim.rows[0].status !== 'PENDING') return;
      const value = claim.rows[0];
      const matches = await client.query(
        `SELECT r.id FROM membership_records r WHERE r.record_status='ACTIVE'
          AND lower(btrim(r.full_name))=lower($1) AND r.phone=$2
          AND NOT EXISTS (SELECT 1 FROM membership_verifications v WHERE v.membership_record_id=r.id
            AND v.status='VERIFIED_MEMBER' AND v.user_id<>$3)
         ORDER BY r.id LIMIT 2`, [value.user_name, value.user_phone, userId]
      );
      // An exact candidate remains pending for a human decision; uncertain or
      // conflicting cases move to internal review. Neither outcome reaches the claimant.
      if (matches.rows.length !== 1) {
        await client.query(`UPDATE membership_claims SET status='REVIEW',updated_at=$2 WHERE id=$1 AND status='PENDING'`, [claimId, Date.now()]);
      }
    });
  } catch (_) {
    // Leave the claim pending for administrator review if classification cannot complete.
  }
}

router.get('/membership/status', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const now = Date.now();
    const status = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
      if (!await userPermission(client, req.user.id, 'membership.status.self.read', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
      return currentVerification(client, req.user.id, now);
    });
    return res.json({ status: status.status, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.get('/access/permissions', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const now = Date.now();
    const result = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
      if (!await userPermission(client, req.user.id, 'access.permissions.self.read', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
      const effective = await listEffectivePermissions(client, req.user.id, now);
      return { ...effective, pilot: !!(await client.query('SELECT 1 FROM pilot_bootstrap WHERE singleton=TRUE AND user_id=$1', [req.user.id])).rows.length };
    });
    return res.json({ permissions: result.permissions, grants: result.grants, server_time: now, pilot: result.pilot });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/claims', requirePhaseB, requireAuth, checkClaimRateLimit, async (req, res) => {
  try {
    const { request_id: requestId, name, phone } = req.body || {};
    if (!validUuid(requestId) || typeof name !== 'string' || !name.trim() || name.trim().length > 128) {
      throw new PhaseBError(400, 'INVALID_CLAIM', 'A valid request_id, name, and phone are required');
    }
    const normalizedPhone = authRouter.validateAndNormalizePhone(phone);
    if (!normalizedPhone) throw new PhaseBError(400, 'INVALID_CLAIM', 'A valid request_id, name, and phone are required');
    const userName = name.trim().replace(/\s+/g, ' ');
    const received = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id);
      // A parallel first claim can create grants while this request waits for
      // the account lock. Check their effectiveness at the time of the check.
      const now = Date.now();
      if (!await userPermission(client, req.user.id, 'membership.claim.self.submit', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
      // Serialize intake for this account before checking request replays.
      const existingVerification = await currentVerification(client, req.user.id, now, true);
      const replay = await client.query('SELECT id,user_id FROM membership_claims WHERE request_id=$1', [requestId]);
      if (replay.rows.length) {
        if (replay.rows[0].user_id !== req.user.id) throw new PhaseBError(409, 'REQUEST_ID_USED', 'Request could not be accepted');
        return { claimId: null };
      }
      const pending = await client.query(`SELECT 1 FROM membership_claims
        WHERE user_id=$1 AND status IN ('PENDING','REVIEW') LIMIT 1`, [req.user.id]);
      if (pending.rows.length) return { claimId: null };
      // The intake transaction already holds the account lock from the grant check.
      const account = await readMembershipAccount(client, req.user.id);
      const claimStatus = matchesMembershipIdentity(account, userName, normalizedPhone) ? 'PENDING' : 'REVIEW';
      const claimId = newId();
      const inserted = await client.query(
        `INSERT INTO membership_claims (id,user_id,request_id,user_name,user_phone,status,created_at,updated_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$7) ON CONFLICT (request_id) DO NOTHING RETURNING id`,
        [claimId, req.user.id, requestId, userName, normalizedPhone, claimStatus, now]
      );
      if (!inserted.rows.length) throw new PhaseBError(409, 'REQUEST_ID_USED', 'Request could not be accepted');
      // Claim intake has one neutral status. Matching happens only after the
      // receipt is sent, and never changes a verified link automatically.
      if (existingVerification.status !== 'VERIFIED_MEMBER') {
        const nextStatus = 'VERIFICATION_PENDING';
        const changed = await client.query(
          `UPDATE membership_verifications SET status=$2, membership_record_id=NULL,
             revision=revision+1, updated_at=$3 WHERE user_id=$1 RETURNING status`,
          [req.user.id, nextStatus, now]
        );
        await client.query(
          `INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
           VALUES ($1,$2,$3,$4,$5,$6,'CLAIM_SUBMITTED',$7)`,
          [newId(), req.user.id, claimId, existingVerification.status, changed.rows[0].status, req.user.id, now]
        );
      }
      return { claimId: claimStatus === 'PENDING' ? claimId : null };
    });
    if (!received) throw new Error('Claim transaction returned no receipt');
    if (received.claimId) {
      res.once('finish', () => setImmediate(() => classifyClaim(req.user.id, received.claimId)));
    }
    return res.status(202).json({ received: true, request_id: requestId, message: 'Your request has been received for review.' });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/identity-reconciliation', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const { request_id: requestId, proposed_name: proposedName, proposed_phone: proposedPhone } = req.body || {};
    const cleanName = typeof proposedName === 'string' ? proposedName.trim().replace(/\s+/g, ' ') : '';
    const normalizedPhone = authRouter.validateAndNormalizePhone(proposedPhone);
    if (!validUuid(requestId) || !cleanName || cleanName.length > 128 || !normalizedPhone) {
      throw new PhaseBError(400, 'INVALID_RECONCILIATION_REQUEST', 'A request ID and valid identity details are required');
    }
    const now = Date.now();
    const responseRequestId = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
      if (!await userPermission(client, req.user.id, 'membership.claim.self.submit', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
      const account = await readMembershipAccount(client, req.user.id, true);
      if (!account || !account.is_active) throw new PhaseBError(404, 'NOT_FOUND', 'Account not found');
      const replay = await client.query('SELECT user_id FROM membership_identity_reconciliation_requests WHERE request_id=$1', [requestId]);
      if (replay.rows.length) return requestId;
      const verification = await currentVerification(client, req.user.id, now, true);
      if (verification.status === 'VERIFIED_MEMBER') {
        throw new PhaseBError(409, 'RECONCILIATION_UNAVAILABLE', 'This account is not eligible for identity reconciliation');
      }
      if (account.registered_name) {
        throw new PhaseBError(409, 'RECONCILIATION_UNAVAILABLE', 'This account is not eligible for identity reconciliation');
      }
      const open = await client.query(
        `SELECT request_id FROM membership_identity_reconciliation_requests
          WHERE user_id=$1 AND status IN ('PENDING','CORRECTION_REQUIRED') LIMIT 1`, [req.user.id]
      );
      if (open.rows.length) return open.rows[0].request_id;
      const id = newId();
      await client.query(
        `INSERT INTO membership_identity_reconciliation_requests
          (id,request_id,user_id,proposed_name,proposed_phone,source,status,submitted_at,updated_at)
         VALUES ($1,$2,$3,$4,$5,'USER_SUBMITTED','PENDING',$6,$6)`,
        [id, requestId, req.user.id, cleanName, normalizedPhone, now]
      );
      await client.query(
        `INSERT INTO membership_identity_reconciliation_revisions
          (reconciliation_id,revision,proposed_name,proposed_phone,actor_id,source,created_at)
         VALUES ($1,1,$2,$3,$4,'USER_SUBMITTED',$5)`, [id, cleanName, normalizedPhone, req.user.id, now]
      );
      await client.query(
        `INSERT INTO membership_identity_reconciliation_events (id,reconciliation_id,actor_id,event,reason,created_at)
         VALUES ($1,$2,$3,'SUBMITTED','USER_SUBMITTED',$4)`, [newId(), id, req.user.id, now]
      );
      return requestId;
    });
    return res.status(202).json({ received: true, request_id: responseRequestId, message: 'Your identity review request has been received.' });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/identity-reconciliation/:id/revise', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const reconciliationId = req.params.id;
    const { proposed_name: proposedName, proposed_phone: proposedPhone } = req.body || {};
    const cleanName = typeof proposedName === 'string' ? proposedName.trim().replace(/\s+/g, ' ') : '';
    const normalizedPhone = authRouter.validateAndNormalizePhone(proposedPhone);
    if (!validUuid(reconciliationId) || !cleanName || cleanName.length > 128 || !normalizedPhone) {
      throw new PhaseBError(400, 'INVALID_RECONCILIATION_REQUEST', 'A valid identity revision is required');
    }
    const now = Date.now();
    const responseRequestId = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
      if (!await userPermission(client, req.user.id, 'membership.claim.self.submit', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
      const { rows: [request] } = await client.query(
        `SELECT id,request_id,status FROM membership_identity_reconciliation_requests
          WHERE id=$1 AND user_id=$2 FOR UPDATE`, [reconciliationId, req.user.id]
      );
      if (!request || request.status !== 'CORRECTION_REQUIRED') {
        throw new PhaseBError(409, 'RECONCILIATION_UNAVAILABLE', 'This identity request cannot be revised');
      }
      const { rows: [latest] } = await client.query(
        'SELECT COALESCE(MAX(revision),0)::int AS revision FROM membership_identity_reconciliation_revisions WHERE reconciliation_id=$1', [reconciliationId]
      );
      const revision = latest.revision + 1;
      await client.query(
        `INSERT INTO membership_identity_reconciliation_revisions
          (reconciliation_id,revision,proposed_name,proposed_phone,actor_id,source,created_at)
         VALUES ($1,$2,$3,$4,$5,'USER_SUBMITTED',$6)`, [reconciliationId, revision, cleanName, normalizedPhone, req.user.id, now]
      );
      await client.query(
        `UPDATE membership_identity_reconciliation_requests
            SET proposed_name=$2,proposed_phone=$3,status='PENDING',updated_at=$4,
                reviewer_id=NULL,membership_record_id=NULL,decision_reason=NULL
          WHERE id=$1`, [reconciliationId, cleanName, normalizedPhone, now]
      );
      await client.query(
        `INSERT INTO membership_identity_reconciliation_events (id,reconciliation_id,actor_id,event,reason,created_at)
         VALUES ($1,$2,$3,'SUBMITTED','USER_CORRECTION',$4)`, [newId(), reconciliationId, req.user.id, now]
      );
      return request.request_id;
    });
    return res.status(202).json({ received: true, request_id: responseRequestId, message: 'Your identity review request has been updated.' });
  } catch (error) { return asError(res, error); }
});

router.get('/membership/member-resource', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const now = Date.now();
    await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
      const status = await currentVerification(client, req.user.id, now);
      if (status.status !== 'VERIFIED_MEMBER') throw new PhaseBError(403, 'MEMBERSHIP_REQUIRED', 'Verified membership is required');
      if (!await userPermission(client, req.user.id, 'membership.status.self.read', 'global', now)) {
        throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      }
    });
    return res.json({ authorized: true, membership_status: 'VERIFIED_MEMBER', server_time: now });
  } catch (error) { return asError(res, error); }
});

module.exports = router;
