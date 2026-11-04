const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'perazim-secret-key-2026';

function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }

  const token = authHeader.substring(7);
  try {
    const payload = jwt.verify(token, JWT_SECRET);
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
      req.user = jwt.verify(token, JWT_SECRET);
    } catch (err) {
      // Ignore invalid token in optional auth
    }
  }
  next();
}

function signToken(user) {
  return jwt.sign(
    { id: user.id, phone: user.phone, name: user.name, role: user.role, campus_id: user.campus_id },
    JWT_SECRET,
    { expiresIn: '365d' }
  );
}

module.exports = {
  requireAuth,
  optionalAuth,
  signToken
};
