// ============================================================================
// Login Attempt Rate Limiter (Brute-Force Protection for PINs)
// ============================================================================

const WINDOW_MS = 15 * 60 * 1000; // 15-minute sliding window
const MAX_FAILED_ATTEMPTS = 5;

// Map of key -> array of failure timestamps [number]
const failedAttempts = new Map();

function normalizePhone(phone) {
  if (!phone) return null;
  let cleaned = String(phone).replace(/[^0-9+]/g, '');
  if (cleaned.startsWith('0')) {
    cleaned = '+254' + cleaned.substring(1);
  } else if (!cleaned.startsWith('+')) {
    cleaned = '+' + cleaned;
  }
  return cleaned;
}

function cleanOldAttempts(timestamps, now) {
  return timestamps.filter(ts => now - ts < WINDOW_MS);
}

function checkLoginRateLimit(req, res, next) {
  const rawPhone = req.body && req.body.phone ? req.body.phone : null;
  const normPhone = normalizePhone(rawPhone);
  const ip = req.ip || req.connection.remoteAddress || 'unknown';
  const phoneKey = normPhone ? `phone:${normPhone}` : null;
  const ipKey = `ip:${ip}`;
  const now = Date.now();

  // Check phone-based attempts first
  if (phoneKey) {
    const attempts = cleanOldAttempts(failedAttempts.get(phoneKey) || [], now);
    failedAttempts.set(phoneKey, attempts);
    if (attempts.length >= MAX_FAILED_ATTEMPTS) {
      const oldest = attempts[0];
      const remainingMs = Math.max(0, WINDOW_MS - (now - oldest));
      const remainingMinutes = Math.ceil(remainingMs / 60000);

      res.set('Retry-After', String(Math.ceil(remainingMs / 1000)));
      return res.status(429).json({
        error: `Too many failed login attempts for this account. Please wait ${remainingMinutes} minute(s) before trying again.`,
        retryAfterSeconds: Math.ceil(remainingMs / 1000)
      });
    }
  }

  // Check IP-based attempts
  const ipAttempts = cleanOldAttempts(failedAttempts.get(ipKey) || [], now);
  failedAttempts.set(ipKey, ipAttempts);
  if (ipAttempts.length >= MAX_FAILED_ATTEMPTS * 3) { // IP allows more before broad block
    const oldest = ipAttempts[0];
    const remainingMs = Math.max(0, WINDOW_MS - (now - oldest));
    res.set('Retry-After', String(Math.ceil(remainingMs / 1000)));
    return res.status(429).json({
      error: 'Too many failed login attempts from this network. Please wait before trying again.',
      retryAfterSeconds: Math.ceil(remainingMs / 1000)
    });
  }

  next();
}

function recordFailedLogin(phone, ip) {
  const normPhone = normalizePhone(phone);
  const now = Date.now();
  if (normPhone) {
    const phoneKey = `phone:${normPhone}`;
    const attempts = cleanOldAttempts(failedAttempts.get(phoneKey) || [], now);
    attempts.push(now);
    failedAttempts.set(phoneKey, attempts);
  }
  if (ip) {
    const ipKey = `ip:${ip}`;
    const attempts = cleanOldAttempts(failedAttempts.get(ipKey) || [], now);
    attempts.push(now);
    failedAttempts.set(ipKey, attempts);
  }
}

function clearFailedLogins(phone, ip) {
  const normPhone = normalizePhone(phone);
  if (normPhone) failedAttempts.delete(`phone:${normPhone}`);
  if (ip) failedAttempts.delete(`ip:${ip}`);
}

function resetAll() {
  failedAttempts.clear();
}

module.exports = {
  checkLoginRateLimit,
  recordFailedLogin,
  clearFailedLogins,
  resetAll,
  MAX_FAILED_ATTEMPTS,
  WINDOW_MS
};
