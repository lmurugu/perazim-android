'use strict';

const net = require('node:net');
const { PhaseBError, asError, hash, phaseBTransaction } = require('./security');

const ACCOUNT = { scope: 'account', lockNamespace: 91031, limit: 5, windowMs: 60 * 60 * 1000 };
const IP = { scope: 'ip', lockNamespace: 91032, limit: 30, windowMs: 15 * 60 * 1000 };

function normalizeIp(address) {
  if (net.isIP(address) === 4) return address;
  if (net.isIP(address) !== 6 || address.includes('%')) {
    throw new PhaseBError(400, 'INVALID_CLIENT_IP', 'A valid client IP is required');
  }
  const canonical = new URL(`http://[${address}]/`).hostname.slice(1, -1);
  const mapped = /^::ffff:([a-f\d]+):([a-f\d]+)$/.exec(canonical);
  if (!mapped) return canonical;
  const high = parseInt(mapped[1], 16);
  const low = parseInt(mapped[2], 16);
  return [high >> 8, high & 255, low >> 8, low & 255].join('.');
}

async function readBucket(client, policy, key) {
  // Lock the logical bucket even when it has no row yet. Lock namespaces
  // are distinct and callers always take the account lock before the IP lock.
  await client.query('SELECT pg_advisory_xact_lock($1, hashtext($2))', [policy.lockNamespace, key]);
  const { rows: [{ now, attempts }] } = await client.query(
    `SELECT floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint AS now,
            (SELECT attempts FROM membership_claim_rate_limits
              WHERE scope=$1 AND bucket_key=$2) AS attempts`,
    [policy.scope, key]
  );
  const time = Number(now);
  const active = (attempts || []).map(Number).filter(at => at > time - policy.windowMs);
  return { time, active };
}

async function saveAttempt(client, policy, key, bucket, time = bucket.time) {
  const attempts = bucket.active.filter(at => at > time - policy.windowMs);
  attempts.push(time);
  await client.query(
    `INSERT INTO membership_claim_rate_limits (scope,bucket_key,attempts,expires_at)
     VALUES ($1,$2,$3,$4) ON CONFLICT (scope,bucket_key)
     DO UPDATE SET attempts=EXCLUDED.attempts, expires_at=EXCLUDED.expires_at`,
    [policy.scope, key, attempts, Math.max(...attempts) + policy.windowMs]
  );
}

function retryAfter(policy, bucket) {
  if (bucket.active.length < policy.limit) return 0;
  return Math.max(1, Math.ceil((Math.min(...bucket.active) + policy.windowMs - bucket.time) / 1000));
}

async function purgeExpired(client, time) {
  // Reclaim a bounded batch without waiting for another request's row locks.
  await client.query(
    `WITH expired AS (
       SELECT scope,bucket_key FROM membership_claim_rate_limits
       WHERE expires_at <= $1 ORDER BY expires_at
       LIMIT 64 FOR UPDATE SKIP LOCKED
     )
     DELETE FROM membership_claim_rate_limits AS bucket USING expired
     WHERE bucket.scope=expired.scope AND bucket.bucket_key=expired.bucket_key`, [time]
  );
}

async function checkClaimRateLimit(req, res, next) {
  try {
    const accountKey = hash(req.user.id);
    const delay = await phaseBTransaction(async client => {
      const account = await readBucket(client, ACCOUNT, accountKey);
      const accountDelay = retryAfter(ACCOUNT, account);
      if (accountDelay) return accountDelay;

      // Express resolves req.ip using the existing trust-proxy policy.
      const ipKey = hash(normalizeIp(req.ip));
      const ip = await readBucket(client, IP, ipKey);
      const ipDelay = retryAfter(IP, ip);
      // An attempt rejected by the IP limit still counts against its account.
      await saveAttempt(client, ACCOUNT, accountKey, account, ip.time);
      if (!ipDelay) await saveAttempt(client, IP, ipKey, ip);
      await purgeExpired(client, ip.time);
      return ipDelay;
    });
    if (!delay) return next();
    return res.set('Retry-After', String(delay)).set('Cache-Control', 'no-store').status(429).json({
      error: 'Too many membership claim attempts. Please wait before trying again.',
      code: 'RATE_LIMITED',
      retry_after_seconds: delay
    });
  } catch (error) {
    return asError(res, error);
  }
}

module.exports = { checkClaimRateLimit };
