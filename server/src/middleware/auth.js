const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { query } = require('../db');

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

  const token = authHeader.substring(7);
  try {
    const payload = jwt.verify(token, getJwtSecret());
    if (await isTokenRevoked(token)) {
      return res.status(401).json({ error: 'Token has been revoked. Please log in again.' });
    }
    req.user = payload;
    req.token = token;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

async function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7);
    try {
      const payload = jwt.verify(token, getJwtSecret());
      if (!(await isTokenRevoked(token))) {
        req.user = payload;
        req.token = token;
      }
    } catch (err) {
      // Ignore invalid token in optional auth
    }
  }
  next();
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
