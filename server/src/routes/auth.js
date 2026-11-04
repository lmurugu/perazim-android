const express = require('express');
const bcrypt = require('bcryptjs');
const { query } = require('../db');
const { requireAuth, signToken } = require('../middleware/auth');

const router = express.Router();

// Clean Kenyan/International phone number (e.g. 0712345678 -> +254712345678)
function normalizePhone(phone) {
  if (!phone) return '';
  let cleaned = phone.replace(/[^0-9+]/g, '');
  if (cleaned.startsWith('0')) {
    cleaned = '+254' + cleaned.substring(1);
  } else if (!cleaned.startsWith('+')) {
    cleaned = '+' + cleaned;
  }
  return cleaned;
}

// POST /api/auth/register
router.post('/register', async (req, res) => {
  try {
    let { phone, pin, name, campus_id } = req.body;
    if (!phone || !pin || !name) {
      return res.status(400).json({ error: 'Phone number, PIN, and full name are required' });
    }

    const normPhone = normalizePhone(phone);
    if (normPhone.length < 10) {
      return res.status(400).json({ error: 'Please enter a valid phone number' });
    }

    if (pin.length < 4) {
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

    await query(
      'INSERT INTO users (id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)',
      [userId, normPhone, pinHash, name.trim(), campus, 'MEMBER', '', 0, 0, 2, true, now, now]
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
      grace_points: 2
    };

    const token = signToken(userObj);
    return res.status(201).json({ token, user: userObj });
  } catch (err) {
    console.error('Registration error:', err);
    return res.status(500).json({ error: 'Server error during registration' });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  try {
    let { phone, pin } = req.body;
    if (!phone || !pin) {
      return res.status(400).json({ error: 'Phone number and PIN are required' });
    }

    const normPhone = normalizePhone(phone);
    const result = await query('SELECT * FROM users WHERE phone = $1', [normPhone]);
    const user = result.rows && result.rows[0];

    if (!user) {
      return res.status(401).json({ error: 'No account found with this phone number. Please sign up.' });
    }

    const isValid = await bcrypt.compare(pin, user.pin_hash);
    if (!isValid) {
      return res.status(401).json({ error: 'Incorrect PIN. Please try again.' });
    }

    const userObj = {
      id: user.id,
      phone: user.phone,
      name: user.name,
      campus_id: user.campus_id,
      role: user.role,
      avatar_url: user.avatar_url || '',
      xp: user.xp || 0,
      streak: user.streak || 0,
      grace_points: user.grace_points || 2
    };

    const token = signToken(userObj);
    return res.json({ token, user: userObj });
  } catch (err) {
    console.error('Login error:', err);
    return res.status(500).json({ error: 'Server error during login' });
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
        xp: user.xp || 0,
        streak: user.streak || 0,
        grace_points: user.grace_points || 2
      }
    });
  } catch (err) {
    console.error('Fetch me error:', err);
    return res.status(500).json({ error: 'Server error' });
  }
});

module.exports = router;
