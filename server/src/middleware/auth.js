const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { query, isPostgresActive, memoryStore } = require('../db');

function getJwtSecret() {
  const secret = process.env.JWT_SECRET;
  const isProd = process.env.NODE_ENV === 'production' || process.env.REQUIRE_SECURE_ENV === 'true';

  if (!secret) {
    if (isProd) {
      throw new Error('FATAL: A secure JWT_SECRET environment variable (minimum 32 characters) is strictly required in production.');
    }
    if (!global.__ephemeralJwtSecret) {
      global.__ephemeralJwtSecret = crypto.randomBytes(32).toString('hex');
      console.warn('⚠️ WARNING: No JWT_SECRET set. Using ephemeral random key for this session. Set JWT_SECRET in production.');
    }
    return global.__ephemeralJwtSecret;
  }

  if (isProd && (secret.length < 32 || secret === 'perazim-secret-key-2026')) {
    throw new Error('FATAL: JWT_SECRET in production must be at least 32 characters and cannot use default placeholders.');
  }

  return secret;
}

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

async function isTokenRevoked(token) {
  const tokenHash = hashToken(token);
  const result = await query('SELECT token_hash FROM revoked_tokens WHERE token_hash = $1', [tokenHash]);
  return !!(result.rows && result.rows.length > 0);
}

async function revokeToken(token, userId = null) {
  const tokenHash = hashToken(token);
  let expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
  try {
    const decoded = jwt.decode(token);
    if (decoded && decoded.exp) {
      expiresAt = decoded.exp * 1000;
    }
  } catch (e) {}

  await query(
    'INSERT INTO revoked_tokens (token_hash, user_id, expires_at, revoked_at) VALUES ($1, $2, $3, $4) ON CONFLICT (token_hash) DO NOTHING',
    [tokenHash, userId, expiresAt, Date.now()]
  );
  return tokenHash;
}

async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }

  const token = authHeader.substring(7).trim();
  try {
    const payload = jwt.verify(token, getJwtSecret());
    if (await isTokenRevoked(token)) {
      return res.status(401).json({ error: 'Token has been revoked. Please log in again.' });
    }
    const current = await currentAccount(payload);
    if (!current) return res.status(401).json({ error: 'Account is unavailable. Please log in again.' });
    req.user = current;
    req.token = token;
    const gate = await privacyAcknowledgementGate(req, current.id);
    if (gate) return res.status(428).json({ code: 'PRIVACY_NOTICE_ACK_REQUIRED', version: gate.version, sha256: gate.content_hash });
    next();
  } catch (err) {
    if (err && err.code === 'PRIVACY_GATE_UNAVAILABLE') return res.status(503).json({ error: 'Privacy acknowledgement status is temporarily unavailable.' });
    if (err && err.code === 'AUTH_ACCOUNT_UNAVAILABLE') return res.status(401).json({ error: 'Account is unavailable. Please log in again.' });
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

async function privacyAcknowledgementGate(req, userId) {
  const path = String(req.originalUrl || req.baseUrl + req.path || '').split('?')[0];
  const allowed = new Set([
    '/api/auth/logout', '/api/privacy/notice/current', '/api/privacy/public-policy',
    '/api/privacy/presentation', '/api/privacy/acknowledge', '/api/privacy/requests'
  ]);
  if (allowed.has(path) || path.startsWith('/api/privacy/requests/')) return null;
  if (!isPostgresActive()) {
    if (process.env.NODE_ENV !== 'test') return null;
    const notice = memoryStore.data01TestPrivacyNotice;
    if (!notice || notice.publication_status !== 'PUBLISHED' || Number(notice.effective_at) > Date.now() || notice.withdrawn_at) return null;
    const acknowledged = memoryStore.privacy_notice_acknowledgements.some(row => row.user_id === userId && row.notice_version === notice.version && row.content_hash === notice.content_hash);
    return acknowledged ? null : notice;
  }
  try {
    const current = await query(
      `SELECT version,content_hash FROM privacy_notice_versions
        WHERE is_active=TRUE AND publication_status='PUBLISHED' AND withdrawn_at IS NULL AND effective_at <= $1 LIMIT 1`, [Date.now()]
    );
    if (!current.rows.length) return null;
    const notice = current.rows[0];
    const ack = await query(
      `SELECT 1 FROM privacy_notice_acknowledgements
        WHERE user_id=$1 AND notice_version=$2 AND content_hash=$3 LIMIT 1`,
      [userId, notice.version, notice.content_hash]
    );
    return ack.rows.length ? null : notice;
  } catch (_) {
    const error = new Error('Privacy gate unavailable');
    error.code = 'PRIVACY_GATE_UNAVAILABLE';
    throw error;
  }
}

async function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7).trim();
    try {
      const payload = jwt.verify(token, getJwtSecret());
      if (!(await isTokenRevoked(token))) {
        const current = await currentAccount(payload);
        if (current) {
          req.user = current;
          req.token = token;
        }
      }
    } catch (err) {
      // Ignore invalid token in optional auth
    }
  }
  next();
}

async function currentAccount(payload) {
  const result = await query('SELECT id, phone, name, role, campus_id, is_active FROM users WHERE id = $1', [payload.id]);
  const user = result.rows && result.rows[0];
  if (!user || user.is_active === false) return null;

  // The B5 recovery cutoff is enabled only in isolated synthetic Phase B servers.
  // Legacy deployments without migration 0002 retain their existing auth schema.
  if (process.env.NODE_ENV === 'test' && process.env.PHASE_B_SYNTHETIC === 'true' && isPostgresActive()) {
    const security = await query('SELECT auth_not_before FROM admin_security WHERE user_id = $1', [user.id]);
    const notBefore = security.rows && security.rows[0] ? Number(security.rows[0].auth_not_before) : 0;
    if (notBefore > 0 && (!payload.iat || payload.iat * 1000 < notBefore)) return null;
  }

  return {
    id: user.id,
    phone: user.phone,
    name: user.name,
    role: user.role,
    campus_id: user.campus_id
  };
}

function signToken(user) {
  return jwt.sign(
    { id: user.id, phone: user.phone, name: user.name, role: user.role, campus_id: user.campus_id },
    getJwtSecret(),
    { expiresIn: '30d' }
  );
}

module.exports = {
  getJwtSecret,
  requireAuth,
  optionalAuth,
  signToken,
  revokeToken,
  hashToken
};
