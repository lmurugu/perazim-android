'use strict';

const express = require('express');
const { requireAuth } = require('../middleware/auth');
const authRouter = require('./auth');
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

router.post('/membership/claims', requirePhaseB, requireAuth, async (req, res) => {
  try {
    const { request_id: requestId, name, phone } = req.body || {};
    if (!validUuid(requestId) || typeof name !== 'string' || !name.trim() || name.trim().length > 128) {
      throw new PhaseBError(400, 'INVALID_CLAIM', 'A valid request_id, name, and phone are required');
    }
    const normalizedPhone = authRouter.validateAndNormalizePhone(phone);
    if (!normalizedPhone) throw new PhaseBError(400, 'INVALID_CLAIM', 'A valid request_id, name, and phone are required');
    const userName = name.trim().replace(/\s+/g, ' ');
    const now = Date.now();
    const received = await phaseBTransaction(async client => {
      await ensureSelfServiceGrants(client, req.user.id, now);
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
      const claimId = newId();
      const inserted = await client.query(
        `INSERT INTO membership_claims (id,user_id,request_id,user_name,user_phone,status,created_at,updated_at)
         VALUES ($1,$2,$3,$4,$5,'PENDING',$6,$6) ON CONFLICT (request_id) DO NOTHING RETURNING id`,
        [claimId, req.user.id, requestId, userName, normalizedPhone, now]
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
      return { claimId };
    });
    if (!received) throw new Error('Claim transaction returned no receipt');
    res.once('finish', () => setImmediate(() => classifyClaim(req.user.id, received.claimId)));
    return res.status(202).json({ received: true, request_id: requestId, message: 'Your request has been received for review.' });
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
