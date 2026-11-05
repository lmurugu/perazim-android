const jwt = require('jsonwebtoken');
const crypto = require('crypto');

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

function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }

  const token = authHeader.substring(7);
  try {
    const payload = jwt.verify(token, getJwtSecret());
    req.user = payload;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7);
    try {
      req.user = jwt.verify(token, getJwtSecret());
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
    { expiresIn: '365d' }
  );
}

module.exports = {
  getJwtSecret,
  requireAuth,
  optionalAuth,
  signToken
};
