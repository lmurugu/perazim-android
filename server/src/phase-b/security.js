'use strict';

const crypto = require('crypto');
const net = require('net');
const { isPostgresActive, withTransaction } = require('../db');

class PhaseBError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function phaseBDatabaseAllowed() {
  if (process.env.NODE_ENV !== 'test' || process.env.PHASE_B_SYNTHETIC !== 'true' || !isPostgresActive()) return false;
  try {
    const url = new URL(process.env.DATABASE_URL || '');
    const host = url.hostname.toLowerCase();
    const local = host === 'localhost' || host === '::1' || host === '[::1]' || net.isIP(host) === 4 && host.startsWith('127.');
    const database = decodeURIComponent(url.pathname.replace(/^\//, ''));
    const approvedDatabase = /^perazim_phaseb_[a-z0-9]{6,32}_test$/.test(database);
    return local && approvedDatabase;
  } catch (_) {
    return false;
  }
}

function isLocalDatabaseAddress(value) {
  let address = String(value || '').split('/')[0].toLowerCase().replace(/^::ffff:/, '');
  if (address === '::1' || address.startsWith('fc') || address.startsWith('fd') || address.startsWith('fe80:')) return true;
  if (net.isIP(address) !== 4) return false;
  const octets = address.split('.').map(Number);
  return octets[0] === 10 || octets[0] === 127 || octets[0] === 169 && octets[1] === 254 ||
    octets[0] === 192 && octets[1] === 168 || octets[0] === 172 && octets[1] >= 16 && octets[1] <= 31;
}

function requirePhaseB(req, res, next) {
  res.set('Cache-Control', 'private, no-store');
  res.set('Pragma', 'no-cache');
  phaseBTransaction(async () => true).then(() => next()).catch(error => asError(res, error));
}

function requireDatabase() {
  if (!phaseBDatabaseAllowed()) throw new PhaseBError(503, 'PHASE_B_UNAVAILABLE', 'Phase B synthetic environment unavailable');
}

async function verifyDatabaseHandle(client) {
  requireDatabase();
  const expected = decodeURIComponent(new URL(process.env.DATABASE_URL).pathname.replace(/^\//, ''));
  const result = await client.query('SELECT current_database() AS database, inet_server_addr()::text AS address');
  const actual = result.rows[0] || {};
  if (actual.database !== expected) throw new PhaseBError(503, 'PHASE_B_UNAVAILABLE', 'Phase B synthetic environment unavailable');
  if (actual.address && !isLocalDatabaseAddress(actual.address)) throw new PhaseBError(503, 'PHASE_B_UNAVAILABLE', 'Phase B synthetic environment unavailable');
}

function hash(value) {
  return crypto.createHash('sha256').update(String(value)).digest('hex');
}

function newId() {
  return crypto.randomUUID();
}

function asError(res, error) {
  if (error instanceof PhaseBError) return res.status(error.status).json({ error: error.message, code: error.code });
  if (error && error.code === '23505') return res.status(409).json({ error: 'Request conflicts with existing data', code: 'CONFLICT' });
  // Avoid returning SQL, paths, register data, or cryptographic details.
  return res.status(500).json({ error: 'Request could not be completed', code: 'INTERNAL_ERROR' });
}

async function phaseBTransaction(callback) {
  requireDatabase();
  return withTransaction(async client => {
    await verifyDatabaseHandle(client);
    return callback(client);
  });
}

async function userPermission(client, userId, code, scope = 'global', now = Date.now(), lock = false) {
  const result = await client.query(
    `SELECT 1 FROM user_permissions up JOIN permissions p ON p.code=up.permission_code
      WHERE up.user_id=$1 AND up.permission_code=$2 AND p.active=TRUE
        AND up.revoked_at IS NULL AND up.effective_at <= $3
        AND (up.expires_at IS NULL OR up.expires_at > $3)
        AND (up.scope='global' OR up.scope=$4) LIMIT 1${lock ? ' FOR SHARE OF up,p' : ''}`,
    [userId, code, now, scope]
  );
  return result.rows.length > 0;
}

async function ensureSelfServiceGrants(client, userId, now = Date.now()) {
  const account = await client.query('SELECT is_active FROM users WHERE id=$1 FOR UPDATE', [userId]);
  if (!account.rows.length || !account.rows[0].is_active) throw new PhaseBError(401, 'ACCOUNT_UNAVAILABLE', 'Account is unavailable');
  for (const code of ['membership.status.self.read', 'membership.claim.self.submit', 'access.permissions.self.read']) {
    const prior = await client.query('SELECT 1 FROM user_permissions WHERE user_id=$1 AND permission_code=$2 LIMIT 1', [userId, code]);
    if (prior.rows.length) continue;
    await client.query(
      `INSERT INTO user_permissions (id,user_id,permission_code,scope,granted_by,granted_at,effective_at,expires_at,revoked_at,revoked_by)
       VALUES ($1,$2,$3,'global',$2,$4,$4,NULL,NULL,NULL)`, [newId(), userId, code, now]
    );
  }
}

async function listEffectivePermissions(client, userId, now = Date.now()) {
  const grants = await client.query(
    `SELECT up.id, up.permission_code AS code, up.scope, up.effective_at, up.expires_at
       FROM user_permissions up JOIN permissions p ON p.code=up.permission_code
      WHERE up.user_id=$1 AND p.active=TRUE AND up.revoked_at IS NULL
        AND up.effective_at <= $2 AND (up.expires_at IS NULL OR up.expires_at > $2)
      ORDER BY up.permission_code, up.scope, up.granted_at, up.id`, [userId, now]
  );
  return { grants: grants.rows, permissions: [...new Set(grants.rows.map(row => row.code))].sort() };
}

function safeReason(value) {
  return typeof value === 'string' && /^[A-Z][A-Z0-9_]{2,39}$/.test(value) ? value : null;
}

async function writeAudit(client, { actorId, action, resourceType, resourceId = '', outcome = 'SUCCESS', reason = 'OWNER_APPROVED', requestId = null, createdAt = Date.now() }) {
  const reasonCode = safeReason(reason);
  if (!reasonCode) throw new PhaseBError(400, 'INVALID_REASON', 'Reason must be a short approved reason code');
  await client.query(
    `INSERT INTO admin_audit_log (id,actor_id,action,resource_type,resource_id,outcome,reason,request_id,created_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)`,
    [newId(), actorId, action, resourceType, String(resourceId), outcome, reasonCode, requestId, createdAt]
  );
}

async function findPilot(client, userId, lock = false) {
  const result = await client.query(
    `SELECT pb.user_id, s.security_version, s.auth_not_before, u.is_active
       FROM pilot_bootstrap pb JOIN admin_security s ON s.user_id=pb.user_id
       JOIN users u ON u.id=pb.user_id WHERE pb.singleton=TRUE AND pb.user_id=$1${lock ? ' FOR UPDATE OF pb,s,u' : ''}`,
    [userId]
  );
  return result.rows[0] || null;
}

async function requireAdminAuthority(client, { userId, authToken, privilegedToken, capability, scope = 'global', now = Date.now(), lock = false }) {
  const pilot = await findPilot(client, userId, lock);
  if (!pilot || !pilot.is_active) throw new PhaseBError(403, 'ADMIN_FORBIDDEN', 'Administrator authority is unavailable');
  const session = await client.query(
    `SELECT token_hash FROM privileged_sessions WHERE token_hash=$1 AND user_id=$2
       AND auth_token_hash=$3 AND security_version=$4 AND revoked_at IS NULL AND expires_at>$5${lock ? ' FOR UPDATE' : ''}`,
    [hash(privilegedToken), userId, hash(authToken), pilot.security_version, now]
  );
  if (!session.rows.length) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
  if (capability && !await userPermission(client, userId, capability, scope, now, lock)) throw new PhaseBError(403, 'PERMISSION_DENIED', 'This action is not permitted');
  return pilot;
}

function requireAdmin(capability, scopeFromRequest = () => 'global', options = {}) {
  return async (req, res, next) => {
    try {
      const privilegedToken = req.get('X-Privileged-Session') || '';
      if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
      await phaseBTransaction(async client => {
        await requireAdminAuthority(client, {
          userId: req.user.id,
          authToken: req.token,
          privilegedToken,
          capability,
          scope: scopeFromRequest(req),
          lock: options.lock === true
        });
      });
      req.privilegedToken = privilegedToken;
      next();
    } catch (error) { asError(res, error); }
  };
}

module.exports = {
  PhaseBError,
  asError,
  hash,
  newId,
  phaseBDatabaseAllowed,
  isLocalDatabaseAddress,
  phaseBTransaction,
  requirePhaseB,
  requireDatabase,
  verifyDatabaseHandle,
  requireAdmin,
  requireAdminAuthority,
  userPermission,
  ensureSelfServiceGrants,
  listEffectivePermissions,
  safeReason,
  writeAudit,
  findPilot
};
