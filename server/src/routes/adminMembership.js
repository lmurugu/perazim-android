'use strict';

const express = require('express');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const { requireAuth } = require('../middleware/auth');
const { validateAndNormalizePhone } = require('./auth');
const {
  PhaseBError, asError, hash, newId, phaseBTransaction, requirePhaseB,
  requireAdminAuthority, userPermission, writeAudit, safeReason
} = require('../phase-b/security');
const { decryptSecret, matchingCounter } = require('../phase-b/totp');

const router = express.Router();
router.use(requirePhaseB, requireAuth);
const PRIVILEGED_TTL_MS = 10 * 60 * 1000;
const ADMIN_CAPABILITIES = [
  'membership.record.read', 'membership.register.import', 'membership.claim.review',
  'membership.verification.confirm', 'membership.record.correct',
  'membership.verification.correct', 'membership.verification.revoke',
  'admin.permissions.read', 'admin.permissions.grant', 'admin.permissions.revoke', 'admin.audit.read'
];

function uuid(value) {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}
function requestUuid(value) { return uuid(value); }
function validUserId(value) { return typeof value === 'string' && /^[A-Za-z0-9_-]{1,64}$/.test(value); }
function validReason(value) { return safeReason(value); }
function adminToken(req) { return req.get('X-Privileged-Session') || ''; }

// The existing inbox can cache this neutral message; never include register details.
async function notifyStatusChange(client, userId, now) {
  await client.query(`INSERT INTO in_app_notifications
    (id,user_id,actor_id,actor_name,type,target_id,title,body,is_read,created_at)
    VALUES ($1,$2,'membership_service','Perazim','MEMBERSHIP_STATUS','membership',
      'Membership request update','Open Membership in your profile to check your status.',FALSE,$3)`,
  [newId(), userId, now]);
}
function scopeAllowed(code, scope) {
  if (scope === 'global') return true;
  if (['membership.record.read', 'membership.record.correct'].includes(code)) return uuid(scope);
  if (['membership.claim.review', 'membership.verification.confirm'].includes(code)) return uuid(scope);
  if (['membership.verification.correct', 'membership.verification.revoke'].includes(code)) return validUserId(scope);
  return false;
}

async function authorize(client, req, capability, scope = 'global', lock = true) {
  const privilegedToken = adminToken(req);
  if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
  await requireAdminAuthority(client, {
    userId: req.user.id, authToken: req.token, privilegedToken, capability, scope, lock
  });
}

async function adminWork(req, capability, scope, work) {
  return phaseBTransaction(async client => {
    await authorize(client, req, capability, scope, true);
    return work(client);
  });
}

async function adminReadWork(req, work) {
  return phaseBTransaction(async client => {
    const privilegedToken = adminToken(req);
    if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
    await requireAdminAuthority(client, { userId: req.user.id, authToken: req.token, privilegedToken, capability: null, scope: 'global', lock: true });
    return work(client);
  });
}

async function assertFreshRequest(client, actorId, action, requestId) {
  if (!requestUuid(requestId)) throw new PhaseBError(400, 'INVALID_REQUEST_ID', 'A valid request_id is required');
  const prior = await client.query(
    'SELECT resource_id FROM admin_audit_log WHERE actor_id=$1 AND action=$2 AND request_id=$3 ORDER BY created_at DESC LIMIT 1',
    [actorId, action, requestId]
  );
  if (prior.rows.length) throw new PhaseBError(409, 'REQUEST_ALREADY_PROCESSED', 'This request has already been processed');
}

async function scopedIds(client, userId, code, now = Date.now(), lock = false) {
  const rows = await client.query(
    `SELECT up.scope FROM user_permissions up JOIN permissions p ON p.code=up.permission_code
      WHERE up.user_id=$1 AND up.permission_code=$2 AND p.active=TRUE
      AND up.revoked_at IS NULL AND up.effective_at <= $3 AND (up.expires_at IS NULL OR up.expires_at>$3)${lock ? ' FOR SHARE OF up,p' : ''}`,
    [userId, code, now]
  );
  return rows.rows.map(row => row.scope);
}

async function assertUniqueTargetIsPilot(client, targetUserId) {
  const result = await client.query('SELECT user_id FROM pilot_bootstrap WHERE singleton=TRUE');
  if (!result.rows.length || result.rows[0].user_id !== targetUserId) {
    throw new PhaseBError(403, 'PILOT_ONLY', 'The pilot administrator is the only account eligible for these grants');
  }
}

router.post('/security/step-up', async (req, res) => {
  try {
    const { pin, code } = req.body || {};
    const now = Date.now();
    const session = await phaseBTransaction(async client => {
      const attempts = await client.query(
        `SELECT COUNT(*)::int AS count FROM admin_audit_log f WHERE f.actor_id=$1 AND f.action='ADMIN_STEP_UP_FAILURE'
          AND f.created_at>GREATEST($2,COALESCE((SELECT MAX(s.created_at) FROM admin_audit_log s WHERE s.actor_id=$1 AND s.action='ADMIN_STEP_UP_SUCCESS'),0))`,
        [req.user.id, now - 15 * 60 * 1000]
      );
      if (attempts.rows[0].count >= 5) return { failure: 'RATE_LIMITED' };
      const result = await client.query(
        `SELECT pb.user_id,u.pin_hash,u.is_active,s.totp_secret_cipher,s.totp_last_counter,s.security_version,s.auth_not_before
           FROM pilot_bootstrap pb JOIN users u ON u.id=pb.user_id JOIN admin_security s ON s.user_id=pb.user_id
          WHERE pb.singleton=TRUE AND pb.user_id=$1 FOR UPDATE OF pb,u,s`, [req.user.id]
      );
      const pilot = result.rows[0];
      let valid = !!(pilot && pilot.is_active && now >= Number(pilot.auth_not_before));
      if (valid && (typeof pin !== 'string' || !/^\d{4,12}$/.test(pin) || typeof code !== 'string' || !/^\d{6}$/.test(code))) valid = false;
      if (valid) valid = await bcrypt.compare(pin, pilot.pin_hash);
      if (!valid) {
        await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_STEP_UP_FAILURE', resourceType: 'administrator', resourceId: req.user.id, outcome: 'DENIED', reason: 'AUTH_FAILURE' });
        return { failure: 'STEP_UP_FAILED' };
      }
      let secret;
      try { secret = decryptSecret(pilot.totp_secret_cipher); }
      catch (_) { return { failure: 'STEP_UP_UNAVAILABLE' }; }
      const counter = matchingCounter(secret, code, now, Number(pilot.totp_last_counter));
      if (counter === null) {
        await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_STEP_UP_FAILURE', resourceType: 'administrator', resourceId: req.user.id, outcome: 'DENIED', reason: 'AUTH_FAILURE' });
        return { failure: 'STEP_UP_FAILED' };
      }
      const updated = await client.query(
        `UPDATE admin_security SET totp_last_counter=$2 WHERE user_id=$1 AND totp_last_counter<$2 RETURNING security_version`,
        [req.user.id, counter]
      );
      if (!updated.rows.length) {
        await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_STEP_UP_FAILURE', resourceType: 'administrator', resourceId: req.user.id, outcome: 'DENIED', reason: 'AUTH_FAILURE' });
        return { failure: 'STEP_UP_FAILED' };
      }
      const raw = crypto.randomBytes(32).toString('base64url');
      const expiresAt = now + PRIVILEGED_TTL_MS;
      await client.query(
        `INSERT INTO privileged_sessions (token_hash,user_id,security_version,auth_token_hash,expires_at,revoked_at,created_at)
         VALUES ($1,$2,$3,$4,$5,NULL,$6)`,
        [hash(raw), req.user.id, updated.rows[0].security_version, hash(req.token), expiresAt, now]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_STEP_UP_SUCCESS', resourceType: 'administrator', resourceId: req.user.id, reason: 'AUTH_SUCCESS' });
      return { raw, expiresAt };
    });
    if (session.failure === 'RATE_LIMITED') throw new PhaseBError(429, 'STEP_UP_RATE_LIMITED', 'Too many administrator authentication attempts');
    if (session.failure === 'STEP_UP_UNAVAILABLE') throw new PhaseBError(503, 'STEP_UP_UNAVAILABLE', 'Administrator second factor is unavailable');
    if (session.failure) throw new PhaseBError(401, 'STEP_UP_FAILED', 'Administrator authentication failed');
    return res.json({ token: session.raw, expires_at: session.expiresAt, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.get('/membership/claims', async (req, res) => {
  try {
    const now = Date.now();
    const result = await adminReadWork(req, async client => {
      const scopes = await scopedIds(client, req.user.id, 'membership.claim.review', now, true);
      const all = scopes.includes('global');
      const ids = scopes.filter(scope => scope !== 'global' && uuid(scope));
      if (!all && !ids.length) throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      const claims = await client.query(
        `SELECT id,user_id,request_id,user_name,user_phone,status,created_at,updated_at
           FROM membership_claims WHERE status IN ('PENDING','REVIEW') AND ($1::boolean OR id=ANY($2::uuid[]))
          ORDER BY created_at,id LIMIT 200`, [all, ids]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_CLAIMS_READ', resourceType: 'membership_claims', resourceId: '', reason: 'ADMIN_READ' });
      return claims.rows;
    });
    return res.json({ claims: result, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.get('/membership/claims/:id', async (req, res) => {
  try {
    if (!uuid(req.params.id)) throw new PhaseBError(404, 'NOT_FOUND', 'Claim not found');
    const now = Date.now();
    const claim = await adminWork(req, 'membership.claim.review', req.params.id, async client => {
      const result = await client.query(
        `SELECT id,user_id,request_id,user_name,user_phone,status,created_at,updated_at
           FROM membership_claims WHERE id=$1`, [req.params.id]
      );
      if (!result.rows.length) throw new PhaseBError(404, 'NOT_FOUND', 'Claim not found');
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_CLAIM_READ', resourceType: 'membership_claim', resourceId: req.params.id, reason: 'ADMIN_READ' });
      return result.rows[0];
    });
    return res.json({ claim, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.get('/membership/records', async (req, res) => {
  try {
    const now = Date.now();
    const records = await adminReadWork(req, async client => {
      const scopes = await scopedIds(client, req.user.id, 'membership.record.read', now, true);
      const all = scopes.includes('global');
      const ids = scopes.filter(scope => scope !== 'global' && uuid(scope));
      if (!all && !ids.length) throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      const result = await client.query(
        `SELECT id,reference,full_name,phone,record_status,revision,created_at,updated_at
           FROM membership_records WHERE ($1::boolean OR id=ANY($2::uuid[]))
          ORDER BY reference,id LIMIT 500`, [all, ids]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_RECORDS_READ', resourceType: 'membership_records', resourceId: '', reason: 'ADMIN_READ' });
      return result.rows;
    });
    return res.json({ records, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/import', async (req, res) => {
  try {
    const { source, records, request_id: requestId, reason } = req.body || {};
    const reasonCode = validReason(reason);
    if (typeof source !== 'string' || !source.trim() || source.trim().length > 100 || !Array.isArray(records) || records.length < 1 || records.length > 10000 || !reasonCode) {
      throw new PhaseBError(400, 'INVALID_IMPORT', 'A source, records array, and approved reason code are required');
    }
    const normalized = [];
    const seen = new Set();
    for (const record of records) {
      const reference = typeof record?.reference === 'string' ? record.reference.trim() : '';
      const fullName = typeof record?.full_name === 'string' ? record.full_name.trim().replace(/\s+/g, ' ') : '';
      const phone = validateAndNormalizePhone(record?.phone);
      if (!reference || reference.length > 100 || !fullName || fullName.length > 128 || !phone) throw new PhaseBError(400, 'INVALID_IMPORT_RECORD', 'One or more records are invalid');
      if (seen.has(reference)) throw new PhaseBError(409, 'DUPLICATE_REFERENCE', 'Import contains duplicate references');
      seen.add(reference);
      normalized.push({ reference, fullName, phone });
    }
    const requestTime = Date.now();
    const importId = await adminWork(req, 'membership.register.import', 'global', async client => {
      await assertFreshRequest(client, req.user.id, 'MEMBERSHIP_IMPORT', requestId);
      const prior = await client.query('SELECT reference FROM membership_records WHERE reference=ANY($1::text[]) LIMIT 1', [normalized.map(item => item.reference)]);
      if (prior.rows.length) throw new PhaseBError(409, 'DUPLICATE_REFERENCE', 'Import references must be new');
      const id = newId();
      const digest = crypto.createHash('sha256').update(JSON.stringify(normalized)).digest('hex');
      await client.query('INSERT INTO membership_imports (id,source,checksum,actor_id,created_at) VALUES ($1,$2,$3,$4,$5)', [id, source.trim(), digest, req.user.id, requestTime]);
      for (const item of normalized) {
        await client.query(
          `INSERT INTO membership_records (id,import_id,reference,full_name,phone,record_status,revision,created_at,updated_at)
           VALUES ($1,$2,$3,$4,$5,'ACTIVE',1,$6,$6)`, [newId(), id, item.reference, item.fullName, item.phone, requestTime]
        );
      }
      await writeAudit(client, { actorId: req.user.id, action: 'MEMBERSHIP_IMPORT', resourceType: 'membership_import', resourceId: id, reason: reasonCode, requestId });
      return id;
    });
    return res.status(201).json({ success: true, import_id: importId, count: normalized.length });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/claims/:id/decision', async (req, res) => {
  try {
    const { decision, record_id: recordId, reason, request_id: requestId } = req.body || {};
    const reasonCode = validReason(reason);
    if (!uuid(req.params.id) || !['APPROVE','REJECT'].includes(decision) || !reasonCode || (decision === 'APPROVE' && !uuid(recordId))) {
      throw new PhaseBError(400, 'INVALID_DECISION', 'A valid decision, reason code, and record_id when approving are required');
    }
    const now = Date.now();
    const action = decision === 'APPROVE' ? 'MEMBERSHIP_CLAIM_APPROVE' : 'MEMBERSHIP_CLAIM_REJECT';
    let resultingStatus = decision === 'APPROVE' ? 'VERIFIED_MEMBER' : 'VERIFICATION_REJECTED';
    await adminWork(req, 'membership.claim.review', req.params.id, async client => {
      await authorize(client, req, 'membership.claim.review', req.params.id, true);
      if (decision === 'APPROVE') await authorize(client, req, 'membership.verification.confirm', req.params.id, true);
      await assertFreshRequest(client, req.user.id, action, requestId);
      const claimResult = await client.query('SELECT * FROM membership_claims WHERE id=$1 FOR UPDATE', [req.params.id]);
      const claim = claimResult.rows[0];
      if (!claim || !['PENDING','REVIEW'].includes(claim.status)) throw new PhaseBError(409, 'CLAIM_UNAVAILABLE', 'This claim cannot be decided');
      let record = null;
      if (decision === 'APPROVE') {
        if (!await userPermission(client, req.user.id, 'membership.record.read', recordId, now, true)) throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
        const recordResult = await client.query(`SELECT id,full_name,phone,record_status FROM membership_records WHERE id=$1 FOR UPDATE`, [recordId]);
        record = recordResult.rows[0];
        if (!record || record.record_status !== 'ACTIVE' || record.phone !== claim.user_phone || record.full_name.toLowerCase().trim() !== claim.user_name.toLowerCase().trim()) {
          throw new PhaseBError(409, 'RECORD_MISMATCH', 'The selected record cannot be confirmed for this claim');
        }
        const linked = await client.query(`SELECT user_id FROM membership_verifications WHERE membership_record_id=$1 AND status='VERIFIED_MEMBER' FOR UPDATE`, [recordId]);
        if (linked.rows.some(row => row.user_id !== claim.user_id)) throw new PhaseBError(409, 'RECORD_ALREADY_LINKED', 'The selected record cannot be confirmed for this claim');
      }
      const existing = await client.query('SELECT status,membership_record_id FROM membership_verifications WHERE user_id=$1 FOR UPDATE', [claim.user_id]);
      const previous = existing.rows[0]?.status || 'REGISTERED';
      if (decision === 'APPROVE' && previous === 'VERIFIED_MEMBER' && existing.rows[0].membership_record_id !== recordId) {
        throw new PhaseBError(409, 'VERIFIED_LINK_EXISTS', 'The existing verified link requires a separate correction or revocation');
      }
      const preserveVerified = decision === 'REJECT' && previous === 'VERIFIED_MEMBER';
      const nextStatus = preserveVerified ? previous : decision === 'APPROVE' ? 'VERIFIED_MEMBER' : 'VERIFICATION_REJECTED';
      resultingStatus = nextStatus;
      if (!preserveVerified) {
        await client.query(
          `INSERT INTO membership_verifications (user_id,membership_record_id,status,revision,updated_at)
           VALUES ($1,$2,$3,1,$4) ON CONFLICT (user_id) DO UPDATE SET membership_record_id=EXCLUDED.membership_record_id,
             status=EXCLUDED.status,revision=membership_verifications.revision+1,updated_at=EXCLUDED.updated_at`,
          [claim.user_id, decision === 'APPROVE' ? recordId : null, nextStatus, now]
        );
      }
      await client.query(`UPDATE membership_claims SET status=$2,updated_at=$3 WHERE id=$1`, [claim.id, decision === 'APPROVE' ? 'APPROVED' : 'REJECTED', now]);
      if (!preserveVerified) {
        await client.query(
          `INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`, [newId(), claim.user_id, claim.id, previous, nextStatus, req.user.id, reasonCode, now]
        );
      }
      await writeAudit(client, { actorId: req.user.id, action, resourceType: 'membership_claim', resourceId: decision === 'APPROVE' ? `${claim.id}:${recordId}` : claim.id, reason: reasonCode, requestId });
      await notifyStatusChange(client, claim.user_id, now);
    });
    return res.json({ success: true, status: resultingStatus });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/users/:id/revoke', async (req, res) => {
  try {
    const userId = req.params.id;
    const { reason, request_id: requestId } = req.body || {};
    const reasonCode = validReason(reason);
    if (!validUserId(userId) || !reasonCode) throw new PhaseBError(400, 'INVALID_REVOCATION', 'A user ID and approved reason code are required');
    const now = Date.now();
    await adminWork(req, 'membership.verification.revoke', userId, async client => {
      await assertFreshRequest(client, req.user.id, 'MEMBERSHIP_VERIFICATION_REVOKE', requestId);
      const current = await client.query('SELECT status,membership_record_id FROM membership_verifications WHERE user_id=$1 FOR UPDATE', [userId]);
      if (!current.rows.length || current.rows[0].status !== 'VERIFIED_MEMBER') throw new PhaseBError(409, 'VERIFICATION_UNAVAILABLE', 'This verification cannot be revoked');
      const priorRecordId = current.rows[0].membership_record_id;
      await client.query(`UPDATE membership_verifications SET status='VERIFICATION_REVOKED',membership_record_id=NULL,revision=revision+1,updated_at=$2 WHERE user_id=$1`, [userId, now]);
      await client.query(
        `INSERT INTO membership_verification_history (id,user_id,previous_status,new_status,actor_id,reason,created_at)
         VALUES ($1,$2,'VERIFIED_MEMBER','VERIFICATION_REVOKED',$3,$4,$5)`, [newId(), userId, req.user.id, reasonCode, now]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'MEMBERSHIP_VERIFICATION_REVOKE', resourceType: 'membership_user', resourceId: `${userId}:${priorRecordId || 'none'}`, reason: reasonCode, requestId });
      await notifyStatusChange(client, userId, now);
    });
    return res.json({ success: true, status: 'VERIFICATION_REVOKED' });
  } catch (error) { return asError(res, error); }
});

router.post('/membership/users/:id/correct', async (req, res) => {
  try {
    const userId = req.params.id;
    const { record_id: recordId, reason, request_id: requestId } = req.body || {};
    const reasonCode = validReason(reason);
    if (!validUserId(userId) || !uuid(recordId) || !reasonCode) throw new PhaseBError(400, 'INVALID_CORRECTION', 'A user ID, record_id, and approved reason code are required');
    const now = Date.now();
    await adminWork(req, 'membership.verification.correct', userId, async client => {
      await assertFreshRequest(client, req.user.id, 'MEMBERSHIP_VERIFICATION_CORRECT', requestId);
      if (!await userPermission(client, req.user.id, 'membership.record.read', recordId, now, true)) throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
      const record = await client.query(`SELECT id,record_status FROM membership_records WHERE id=$1 FOR UPDATE`, [recordId]);
      if (!record.rows.length || record.rows[0].record_status !== 'ACTIVE') throw new PhaseBError(404, 'NOT_FOUND', 'Membership record not found');
      const current = await client.query('SELECT status,membership_record_id FROM membership_verifications WHERE user_id=$1 FOR UPDATE', [userId]);
      const previous = current.rows[0]?.status || 'REGISTERED';
      const priorRecordId = current.rows[0]?.membership_record_id || 'none';
      await client.query(
        `INSERT INTO membership_verifications (user_id,membership_record_id,status,revision,updated_at)
         VALUES ($1,$2,'VERIFIED_MEMBER',1,$3) ON CONFLICT (user_id) DO UPDATE SET membership_record_id=EXCLUDED.membership_record_id,
           status='VERIFIED_MEMBER',revision=membership_verifications.revision+1,updated_at=EXCLUDED.updated_at`, [userId, recordId, now]
      );
      await client.query(
        `INSERT INTO membership_verification_history (id,user_id,previous_status,new_status,actor_id,reason,created_at)
         VALUES ($1,$2,$3,'VERIFIED_MEMBER',$4,$5,$6)`, [newId(), userId, previous, req.user.id, reasonCode, now]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'MEMBERSHIP_VERIFICATION_CORRECT', resourceType: 'membership_user', resourceId: `${userId}:${priorRecordId}:${recordId}`, reason: reasonCode, requestId });
      await notifyStatusChange(client, userId, now);
    });
    return res.json({ success: true, status: 'VERIFIED_MEMBER' });
  } catch (error) { return asError(res, error); }
});

router.patch('/membership/records/:id', async (req, res) => {
  try {
    const recordId = req.params.id;
    const { full_name: fullName, phone, reason, request_id: requestId } = req.body || {};
    const reasonCode = validReason(reason);
    const normalizedPhone = validateAndNormalizePhone(phone);
    if (!uuid(recordId) || typeof fullName !== 'string' || !fullName.trim() || fullName.trim().length > 128 || !normalizedPhone || !reasonCode) {
      throw new PhaseBError(400, 'INVALID_CORRECTION', 'A record ID, valid name and phone, and approved reason code are required');
    }
    const now = Date.now();
    await adminWork(req, 'membership.record.correct', recordId, async client => {
      await assertFreshRequest(client, req.user.id, 'MEMBERSHIP_RECORD_CORRECT', requestId);
      const updated = await client.query(
        `UPDATE membership_records SET full_name=$2,phone=$3,revision=revision+1,updated_at=$4 WHERE id=$1 RETURNING id`,
        [recordId, fullName.trim().replace(/\s+/g, ' '), normalizedPhone, now]
      );
      if (!updated.rows.length) throw new PhaseBError(404, 'NOT_FOUND', 'Membership record not found');
      await writeAudit(client, { actorId: req.user.id, action: 'MEMBERSHIP_RECORD_CORRECT', resourceType: 'membership_record', resourceId: recordId, reason: reasonCode, requestId });
    });
    return res.json({ success: true, record_id: recordId });
  } catch (error) { return asError(res, error); }
});

router.get('/permissions/:userId', async (req, res) => {
  try {
    const userId = req.params.userId;
    if (!validUserId(userId)) throw new PhaseBError(404, 'NOT_FOUND', 'User not found');
    const now = Date.now();
    const grants = await adminWork(req, 'admin.permissions.read', 'global', async client => {
      const result = await client.query(
        `SELECT id,user_id,permission_code AS code,scope,granted_by,granted_at,effective_at,expires_at,revoked_at,revoked_by
           FROM user_permissions WHERE user_id=$1 ORDER BY granted_at,id`, [userId]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_PERMISSIONS_READ', resourceType: 'user_permissions', resourceId: userId, reason: 'ADMIN_READ' });
      return result.rows;
    });
    return res.json({ grants, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.post('/permissions/:userId', async (req, res) => {
  try {
    const userId = req.params.userId;
    const { code, scope = 'global', effective_at: effectiveAt, expires_at: expiresAt, request_id: requestId, reason } = req.body || {};
    const reasonCode = validReason(reason);
    if (!validUserId(userId) || typeof code !== 'string' || !code || typeof scope !== 'string' || !scope || scope === '*' || !reasonCode) {
      throw new PhaseBError(400, 'INVALID_GRANT', 'A user, capability, exact scope, and approved reason code are required');
    }
    const now = Date.now();
    const effective = effectiveAt === undefined ? now : Number(effectiveAt);
    const expires = expiresAt == null ? null : Number(expiresAt);
    if (!Number.isSafeInteger(effective) || effective < now - 60000 || (expires !== null && (!Number.isSafeInteger(expires) || expires <= effective))) {
      throw new PhaseBError(400, 'INVALID_GRANT', 'Grant dates are invalid');
    }
    const grant = await adminWork(req, 'admin.permissions.grant', 'global', async client => {
      await assertFreshRequest(client, req.user.id, 'ADMIN_PERMISSION_GRANT', requestId);
      const known = await client.query('SELECT 1 FROM permissions WHERE code=$1 AND active=TRUE', [code]);
      if (!known.rows.length) throw new PhaseBError(400, 'UNKNOWN_PERMISSION', 'This capability is not assignable');
      if (!scopeAllowed(code, scope)) throw new PhaseBError(400, 'INVALID_SCOPE', 'Scope must be global or one exact resource ID for this capability');
      if (ADMIN_CAPABILITIES.includes(code)) await assertUniqueTargetIsPilot(client, userId);
      else if (!['membership.status.self.read','membership.claim.self.submit','access.permissions.self.read'].includes(code) || userId !== req.user.id) {
        throw new PhaseBError(403, 'PILOT_ONLY', 'Only the pilot may receive administrative capabilities');
      }
      const target = await client.query('SELECT 1 FROM users WHERE id=$1 AND is_active=TRUE', [userId]);
      if (!target.rows.length) throw new PhaseBError(404, 'NOT_FOUND', 'User not found');
      const id = newId();
      await client.query(
        `INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at,expires_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`, [id, userId, code, scope, req.user.id, now, effective, expires]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_PERMISSION_GRANT', resourceType: 'user_permission', resourceId: id, reason: reasonCode, requestId });
      return { id, code, scope, effective_at: effective, expires_at: expires };
    });
    return res.status(201).json({ success: true, grant });
  } catch (error) { return asError(res, error); }
});

router.delete('/permissions/:userId/:grantId', async (req, res) => {
  try {
    const { userId, grantId } = req.params;
    const { request_id: bodyRequestId, reason: bodyReason } = req.body || {};
    const requestId = bodyRequestId || req.query.request_id;
    const reason = bodyReason || req.query.reason;
    const reasonCode = validReason(reason);
    if (!validUserId(userId) || !uuid(grantId) || !reasonCode) throw new PhaseBError(400, 'INVALID_REVOCATION', 'A target, grant ID, and approved reason code are required');
    const now = Date.now();
    await adminWork(req, 'admin.permissions.revoke', 'global', async client => {
      await assertUniqueTargetIsPilot(client, userId);
      await assertFreshRequest(client, req.user.id, 'ADMIN_PERMISSION_REVOKE', requestId);
      const prior = await client.query('SELECT permission_code FROM user_permissions WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL FOR UPDATE', [grantId, userId]);
      if (!prior.rows.length) throw new PhaseBError(404, 'NOT_FOUND', 'Permission grant not found');
      const code = prior.rows[0].permission_code;
      if (ADMIN_CAPABILITIES.includes(code)) await assertUniqueTargetIsPilot(client, userId);
      else if (!['membership.status.self.read','membership.claim.self.submit','access.permissions.self.read'].includes(code) || userId !== req.user.id) {
        throw new PhaseBError(403, 'PILOT_ONLY', 'Only the pilot may receive administrative capabilities');
      }
      const result = await client.query(
        `UPDATE user_permissions SET revoked_at=$3,revoked_by=$4 WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL RETURNING id`,
        [grantId, userId, now, req.user.id]
      );
      if (!result.rows.length) throw new PhaseBError(404, 'NOT_FOUND', 'Permission grant not found');
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_PERMISSION_REVOKE', resourceType: 'user_permission', resourceId: grantId, reason: reasonCode, requestId });
    });
    return res.json({ success: true, grant_id: grantId });
  } catch (error) { return asError(res, error); }
});

router.get('/audit', async (req, res) => {
  try {
    const now = Date.now();
    const events = await adminWork(req, 'admin.audit.read', 'global', async client => {
      const result = await client.query(
        `SELECT id,actor_id,action,resource_type,resource_id,outcome,reason,request_id,created_at
           FROM admin_audit_log ORDER BY created_at DESC,id DESC LIMIT 100`
      );
      await writeAudit(client, { actorId: req.user.id, action: 'ADMIN_AUDIT_READ', resourceType: 'admin_audit_log', resourceId: '', reason: 'ADMIN_READ' });
      return result.rows;
    });
    return res.json({ events, server_time: now });
  } catch (error) { return asError(res, error); }
});

router.post('/security/revoke-sessions', async (req, res) => {
  try {
    const { request_id: requestId, reason, compromised = false } = req.body || {};
    const reasonCode = validReason(reason);
    if (!reasonCode || typeof compromised !== 'boolean') throw new PhaseBError(400, 'INVALID_REVOCATION', 'An approved reason code and compromised flag are required');
    const now = Date.now();
    await phaseBTransaction(async client => {
      await authorize(client, req, 'admin.permissions.revoke', 'global', true);
      await authorize(client, req, 'admin.audit.read', 'global', true);
      await assertFreshRequest(client, req.user.id, 'ADMIN_SESSION_REVOKE', requestId);
      await client.query(`UPDATE admin_security SET security_version=security_version+1,auth_not_before=CASE WHEN $2 THEN $3 ELSE auth_not_before END WHERE user_id=$1`, [req.user.id, compromised, (Math.floor(now / 1000) + 1) * 1000]);
      await client.query('UPDATE privileged_sessions SET revoked_at=$2 WHERE user_id=$1 AND revoked_at IS NULL', [req.user.id, now]);
      if (compromised) {
        await client.query('UPDATE users SET is_active=FALSE,updated_at=$2 WHERE id=$1', [req.user.id, now]);
      }
      await writeAudit(client, { actorId: req.user.id, action: compromised ? 'ADMIN_COMPROMISE_CONTAINMENT' : 'ADMIN_SESSION_REVOKE', resourceType: 'administrator', resourceId: req.user.id, reason: reasonCode, requestId });
    });
    return res.json({ success: true, compromised, sessions_revoked: true });
  } catch (error) { return asError(res, error); }
});

module.exports = router;
