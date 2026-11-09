const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { query, withTransaction } = require('../db');
const { requireAuth, signToken, revokeToken, hashToken } = require('../middleware/auth');
const { checkLoginRateLimit, recordFailedLogin, clearFailedLogins } = require('../middleware/rateLimiter');
const { phaseBDatabaseAllowed, phaseBTransaction, newId } = require('../phase-b/security');
const { getCurrentNotice, recordAcknowledgement } = require('../phase-b/privacyNotice');

const router = express.Router();

function registrationErrorCategory(code) {
  if (typeof code !== 'string' || !/^\d{5}$/.test(code)) return 'INTERNAL';
  if (code.startsWith('08')) return 'STORAGE_UNAVAILABLE';
  if (code.startsWith('23')) return 'DATA_CONFLICT';
  if (code.startsWith('22')) return 'DATA_INVALID';
  return 'DATABASE_ERROR';
}

function logRegistrationFailure(event, error) {
  console.error(JSON.stringify({
    event,
    request_id: crypto.randomUUID(),
    category: registrationErrorCategory(error?.code)
  }));
}

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

    const { privacy_notice_version: noticeVersion, privacy_notice_hash: noticeHash,
      privacy_notice_acknowledged: noticeAcknowledged } = req.body || {};
    let currentNotice;
    try {
      currentNotice = await getCurrentNotice();
    } catch (_) {
      return res.status(503).json({ error: 'Registration is temporarily unavailable. Please try again later.' });
    }
    if (process.env.NODE_ENV === 'production' && currentNotice.publication_status !== 'PUBLISHED') {
      return res.status(503).json({ error: 'Registration is temporarily unavailable until the privacy notice is approved and published.' });
    }
    if (noticeAcknowledged !== true || noticeVersion !== currentNotice.version ||
        typeof noticeHash !== 'string' || noticeHash !== currentNotice.content_hash) {
      return res.status(428).json({ error: 'Please review and acknowledge the current Perazim App Privacy Notice before registering.' });
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

    await withTransaction(async client => {
      await client.query(
        'INSERT INTO users (id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)',
        [userId, normPhone, pinHash, name.trim(), campus, 'MEMBER', '', 0, 0, initialGracePoints, true, now, now]
      );
      await recordAcknowledgement(client, userId, currentNotice, now);
    });

    // Account creation never verifies church membership or examines the private register.
    const membershipStatus = 'REGISTERED';
    if (phaseBDatabaseAllowed()) {
      try {
        await phaseBTransaction(async client => {
          await client.query(
            `INSERT INTO membership_verifications (user_id,membership_record_id,status,verification_source,revision,updated_at)
             VALUES ($1,NULL,$2,$3,1,$4)
             ON CONFLICT (user_id) DO UPDATE SET membership_record_id=NULL,status=EXCLUDED.status,
               verification_source=EXCLUDED.verification_source,revision=membership_verifications.revision+1,updated_at=EXCLUDED.updated_at`,
            [userId, membershipStatus, 'MANUAL', now]
          );
          await client.query(
            `INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
             VALUES ($1,$2,NULL,NULL,$3,$2,$4,$5)`,
            [newId(), userId, membershipStatus, 'ACCOUNT_REGISTERED', now]
          );
        });
      } catch (verificationError) {
        logRegistrationFailure('registration_membership_status_persist_failed', verificationError);
      }
    }

    const userObj = {
      id: userId,
      phone: normPhone,
      name: name.trim(),
      campus_id: campus,
      role: 'MEMBER',
      avatar_url: '',
      xp: 0,
      streak: 0,
      grace_points: (initialGracePoints !== undefined && initialGracePoints !== null) ? Number(initialGracePoints) : 0,
      membership_status: membershipStatus,
      privacy_notice_version: currentNotice.version
    };

    const token = signToken(userObj);
    return res.status(201).json({ token, user: userObj });
  } catch (err) {
    logRegistrationFailure('registration_failed', err);
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
    console.error(JSON.stringify({ event: 'login_failed', category: registrationErrorCategory(err?.code) }));
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
    console.error(JSON.stringify({ event: 'logout_failed', category: registrationErrorCategory(err?.code) }));
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
    console.error(JSON.stringify({ event: 'account_fetch_failed', category: registrationErrorCategory(err?.code) }));
    return res.status(500).json({ error: 'Server error' });
  }
});

router.post('/phone-change', requireAuth, (req, res) => {
  return res.status(501).json({ code: 'PHONE_CHANGE_UNAVAILABLE', error: 'Phone number changes are currently unavailable.' });
});

router.validateAndNormalizePhone = validateAndNormalizePhone;
module.exports = router;
