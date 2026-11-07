'use strict';

const crypto = require('crypto');

function adminKey() {
  const value = process.env.PHASE_B_ADMIN_KEY || '';
  if (/^[0-9a-f]{64}$/i.test(value)) return Buffer.from(value, 'hex');
  const decoded = Buffer.from(value, 'base64');
  if (decoded.length === 32 && decoded.toString('base64').replace(/=+$/, '') === value.replace(/=+$/, '')) return decoded;
  throw new Error('PHASE_B_ADMIN_KEY must encode exactly 32 random bytes');
}

function encryptSecret(secret) {
  const key = adminKey();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  const encrypted = Buffer.concat([cipher.update(secret, 'utf8'), cipher.final()]);
  return `v1:${iv.toString('hex')}:${cipher.getAuthTag().toString('hex')}:${encrypted.toString('hex')}`;
}

function decryptSecret(encoded) {
  const parts = String(encoded || '').split(':');
  if (parts.length !== 4 || parts[0] !== 'v1' || !/^[0-9a-f]{24}$/i.test(parts[1]) || !/^[0-9a-f]{32}$/i.test(parts[2]) || !/^(?:[0-9a-f]{2})+$/i.test(parts[3])) {
    throw new Error('Encrypted TOTP secret has invalid format');
  }
  const decipher = crypto.createDecipheriv('aes-256-gcm', adminKey(), Buffer.from(parts[1], 'hex'));
  decipher.setAuthTag(Buffer.from(parts[2], 'hex'));
  return Buffer.concat([decipher.update(Buffer.from(parts[3], 'hex')), decipher.final()]).toString('utf8');
}

const BASE32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
function base32Encode(bytes) {
  let bits = 0, value = 0, output = '';
  for (const byte of bytes) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      output += BASE32[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits) output += BASE32[(value << (5 - bits)) & 31];
  return output;
}

function base32Decode(text) {
  let bits = 0, value = 0;
  const bytes = [];
  for (const char of String(text).replace(/=+$/, '').toUpperCase()) {
    const digit = BASE32.indexOf(char);
    if (digit < 0) throw new Error('Invalid TOTP secret');
    value = (value << 5) | digit;
    bits += 5;
    if (bits >= 8) {
      bytes.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return Buffer.from(bytes);
}

function generateSecret() {
  return base32Encode(crypto.randomBytes(20));
}

function totpAt(secret, counter) {
  const input = Buffer.alloc(8);
  input.writeBigUInt64BE(BigInt(counter));
  const digest = crypto.createHmac('sha1', base32Decode(secret)).update(input).digest();
  const offset = digest[digest.length - 1] & 0x0f;
  const binary = digest.readUInt32BE(offset) & 0x7fffffff;
  return String(binary % 1000000).padStart(6, '0');
}

function matchingCounter(secret, code, now = Date.now(), lastCounter = -1) {
  if (typeof code !== 'string' || !/^\d{6}$/.test(code)) return null;
  const center = Math.floor(now / 30000);
  let matched = null;
  for (const counter of [center - 1, center, center + 1]) {
    if (counter > lastCounter && crypto.timingSafeEqual(Buffer.from(totpAt(secret, counter)), Buffer.from(code))) matched = counter;
  }
  return matched;
}

module.exports = { adminKey, encryptSecret, decryptSecret, generateSecret, totpAt, matchingCounter };
