const express = require('express');
const bcrypt = require('bcryptjs');
const { query } = require('../db');
const { requireAuth, signToken, revokeToken, hashToken } = require('../middleware/auth');
const { checkLoginRateLimit, recordFailedLogin, clearFailedLogins } = require('../middleware/rateLimiter');

const router = express.Router();

/**
 * Validates and normalizes incoming Kenyan phone numbers.
 * Supported formats:
 * - 10-digit Kenyan format: 07XXXXXXXX or 01XXXXXXXX
 * - E.164 Kenyan format: +2547XXXXXXXX or +2541XXXXXXXX
 * - 12-digit format without leading plus: 2547XXXXXXXX or 2541XXXXXXXX
 * Returns normalized E.164 string (+254XXXXXXXXX) or null if invalid.
 */
function validateAndNormalizePhone(phone) {
  if (!phone || typeof phone !== 'string') return null;
  const cleaned = phone.trim().replace(/[\s\-()]/g, '');

  // 10-digit Kenyan format starting with 07 or 01
  if (/^0[17]\d{8}$/.test(cleaned)) {
    return '+254' + cleaned.substring(1);
  }

  // E.164 Kenyan format (+254 followed by 7 or 1, then 8 digits)
  if (/^\+254[17]\d{8}$/.test(cleaned)) {
    return cleaned;
  }

  // 12-digit format without leading + (254 followed by 7 or 1, then 8 digits)
  if (/^254[17]\d{8}$/.test(cleaned)) {
    return '+' + cleaned;
  }

  return null;
}

function normalizePhone(phone) {
  return validateAndNormalizePhone(phone) || '';
}

// POST /api/auth/register
router.post('/register', async (req, res) => {
  try {
    let { phone, pin, name, campus_id } = req.body;
    if (!phone || !pin || !name) {
      return res.status(400).json({ error: 'Phone number, PIN, and full name are required' });
    }

    const normPhone = validateAndNormalizePhone(phone);
    if (!normPhone) {
      return res.status(400).json({
        error: 'Please enter a valid Kenyan phone number (e.g. 07XXXXXXXX, 01XXXXXXXX, or +254XXXXXXXXX)'
      });
    }

    if (typeof pin !== 'string' || pin.length < 4) {
      return res.status(400).json({ error: 'PIN must be at least 4 digits' });
    }

    // Check existing
    const existing = await query('SELECT * FROM users WHERE phone = $1', [normPhone]);
    if (existing.rows && existing.rows.length > 0) {
      return res.status(409).json({ error: 'An account with this phone number already exists. Please log in.' });
    }

    const pinHash = await bcrypt.hash(pin, 10);
    const userId = 'user_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const now = Date.now();
    const campus = campus_id || 'campus_embu';
    // Starting allowance is server-owned; clients cannot grant themselves points.
    const initialGracePoints = 0;

    await query(
      'INSERT INTO users (id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)',
      [userId, normPhone, pinHash, name.trim(), campus, 'MEMBER', '', 0, 0, initialGracePoints, true, now, now]
    );

    const userObj = {
      id: userId,
      phone: normPhone,
      name: name.trim(),
      campus_id: campus,
      role: 'MEMBER',
      avatar_url: '',
      xp: 0,
      streak: 0,
      grace_points: (initialGracePoints !== undefined && initialGracePoints !== null) ? Number(initialGracePoints) : 0
    };

    const token = signToken(userObj);
    return res.status(201).json({ token, user: userObj });
  } catch (err) {
    console.error('Registration error:', err);
    return res.status(500).json({ error: 'Server error during registration' });
  }
});

// POST /api/auth/login
router.post('/login', checkLoginRateLimit, async (req, res) => {
  try {
    let { phone, pin } = req.body;
    if (!phone || !pin) {
      return res.status(400).json({ error: 'Phone number and PIN are required' });
    }

    const normPhone = validateAndNormalizePhone(phone);
    if (!normPhone) {
      return res.status(400).json({
        error: 'Please enter a valid Kenyan phone number (e.g. 07XXXXXXXX, 01XXXXXXXX, or +254XXXXXXXXX)'
      });
    }

    const ip = req.ip || req.connection.remoteAddress;
    const result = await query('SELECT * FROM users WHERE phone = $1', [normPhone]);
    const user = result.rows && result.rows[0];

    if (!user) {
      recordFailedLogin(normPhone, ip);
      return res.status(401).json({ error: 'No account found with this phone number. Please sign up.' });
    }

    if (user.is_active === false) {
      recordFailedLogin(normPhone, ip);
      return res.status(401).json({ error: 'This account is unavailable. Contact the church office.' });
    }

    const isValid = await bcrypt.compare(pin, user.pin_hash);
    if (!isValid) {
      recordFailedLogin(normPhone, ip);
      return res.status(401).json({ error: 'Incorrect PIN. Please try again.' });
    }

    // Reset failed attempt counter on successful login
    clearFailedLogins(normPhone, ip);

    const userObj = {
      id: user.id,
      phone: user.phone,
      name: user.name,
      campus_id: user.campus_id,
      role: user.role,
      avatar_url: user.avatar_url || '',
      xp: (user.xp !== undefined && user.xp !== null) ? Number(user.xp) : 0,
      streak: (user.streak !== undefined && user.streak !== null) ? Number(user.streak) : 0,
      grace_points: (user.grace_points !== undefined && user.grace_points !== null) ? Number(user.grace_points) : 0
    };

    const token = signToken(userObj);
    return res.json({ token, user: userObj });
  } catch (err) {
    console.error('Login error:', err);
    return res.status(500).json({ error: 'Server error during login' });
  }
});

// POST /api/auth/logout (Token Revocation)
router.post('/logout', requireAuth, async (req, res) => {
  try {
    const token = req.token;
    const tokenHash = await revokeToken(token, req.user ? req.user.id : null);
    return res.json({
      success: true,
      message: 'Logged out successfully. Token revoked.',
      revokedTokenHash: tokenHash
    });
  } catch (err) {
    console.error('Logout error:', err);
    return res.status(500).json({ error: 'Server error during logout' });
  }
});

// GET /api/auth/me
router.get('/me', requireAuth, async (req, res) => {
  try {
    const result = await query('SELECT * FROM users WHERE id = $1', [req.user.id]);
    const user = result.rows && result.rows[0];
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    return res.json({
      user: {
        id: user.id,
        phone: user.phone,
        name: user.name,
        campus_id: user.campus_id,
        role: user.role,
        avatar_url: user.avatar_url || '',
        xp: (user.xp !== undefined && user.xp !== null) ? Number(user.xp) : 0,
        streak: (user.streak !== undefined && user.streak !== null) ? Number(user.streak) : 0,
        grace_points: (user.grace_points !== undefined && user.grace_points !== null) ? Number(user.grace_points) : 0
      }
    });
  } catch (err) {
    console.error('Fetch me error:', err);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.validateAndNormalizePhone = validateAndNormalizePhone;
module.exports = router;
