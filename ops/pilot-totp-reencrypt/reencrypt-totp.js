#!/usr/bin/env node
// Re-encrypts stored admin TOTP secrets (admin_security.totp_secret_cipher) from an OLD
// PHASE_B_ADMIN_KEY to a NEW one. It reuses the server's own encryptSecret/decryptSecret, so the
// stored format is unchanged. It writes only totp_secret_cipher. Counters, security_version,
// auth_not_before and sessions are not touched.
//
// Default is a dry run: it decrypts every row with the OLD key and reports counts, writes nothing.
// --apply re-encrypts every row in ONE transaction. Any failure rolls back everything.
// No secret value is printed. Keys come from environment variables only.
//
// Usage:
//   DATABASE_URL=postgresql://... OLD_PHASE_B_ADMIN_KEY=... NEW_PHASE_B_ADMIN_KEY=... \
//     node reencrypt-totp.js                       # dry run
//   ... node reencrypt-totp.js --apply --confirm=<database name> PRE_CHANGE_DUMP=/path/to/dump
// With zero rows, both modes only validate the NEW key. Then the new key can be set in the env file.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const SERVER = process.env.CLONE_SERVER || path.resolve(__dirname, '../../server');
const { Pool } = require(path.join(SERVER, 'node_modules/pg'));
const { encryptSecret, decryptSecret, adminKey } = require(path.join(SERVER, 'src/phase-b/totp.js'));

const die = msg => { throw new Error(msg); };
const WELL_FORMED = /^v1:[0-9a-f]{24}:[0-9a-f]{32}:([0-9a-f]{2})+$/;

function withKey(value, fn) {
  const previous = process.env.PHASE_B_ADMIN_KEY;
  process.env.PHASE_B_ADMIN_KEY = value;
  try { return fn(); } finally {
    if (previous === undefined) delete process.env.PHASE_B_ADMIN_KEY; else process.env.PHASE_B_ADMIN_KEY = previous;
  }
}

async function main() {
  const args = process.argv.slice(2);
  const apply = args.includes('--apply');
  const databaseUrl = process.env.DATABASE_URL || die('DATABASE_URL is required');
  const oldKey = process.env.OLD_PHASE_B_ADMIN_KEY || die('OLD_PHASE_B_ADMIN_KEY is required');
  const newKey = process.env.NEW_PHASE_B_ADMIN_KEY || die('NEW_PHASE_B_ADMIN_KEY is required');
  // Both keys must be valid 32-byte keys in the server's format. The check does not print them.
  withKey(oldKey, () => adminKey());
  withKey(newKey, () => adminKey());
  if (oldKey === newKey) die('OLD and NEW keys are identical; nothing to do');

  const target = decodeURIComponent(new URL(databaseUrl).pathname.slice(1));
  if (apply) {
    if (args.find(a => a.startsWith('--confirm=')) !== `--confirm=${target}`) die(`--apply needs --confirm=${target}`);
    if (!['localhost', '127.0.0.1', '::1'].includes(new URL(databaseUrl).hostname)) die('writes are limited to a local database');
    const dump = process.env.PRE_CHANGE_DUMP || die('PRE_CHANGE_DUMP must name a fresh dump taken before --apply');
    execFileSync('pg_restore', ['--list', dump], { stdio: 'ignore' });
    execFileSync('sha256sum', ['--check', `${dump}.sha256`], { stdio: 'ignore' });
  }

  const pool = new Pool({ connectionString: databaseUrl });
  const client = await pool.connect();
  let plain = [];
  try {
    await client.query('BEGIN');
    const rows = (await client.query('SELECT user_id, totp_secret_cipher FROM admin_security ORDER BY user_id FOR UPDATE')).rows;
    console.log(`admin_security rows: ${rows.length}`);
    if (rows.length === 0) {
      console.log('no encrypted TOTP secrets stored: no re-encryption needed; the NEW key can be set (bootstrap path).');
    }
    // Decrypt every row with the OLD key first. Any failure stops the run before a write.
    plain = rows.map(r => {
      if (!WELL_FORMED.test(r.totp_secret_cipher)) die('a stored secret has an unexpected format; nothing changed');
      let secret;
      try { secret = withKey(oldKey, () => decryptSecret(r.totp_secret_cipher)); }
      catch (_) { die('a stored secret does not decrypt with OLD key; nothing changed'); }
      if (!secret) die('a stored secret decrypted to an empty value; nothing changed');
      return { user_id: r.user_id, old: r.totp_secret_cipher, secret };
    });
    console.log(`decrypted with OLD key: ${plain.length} of ${rows.length}`);

    if (!apply) {
      await client.query('ROLLBACK');
      console.log('DRY RUN: nothing written. Re-run with --apply --confirm=<db> after a fresh dump.');
    } else {
      for (const p of plain) {
        const fresh = withKey(newKey, () => encryptSecret(p.secret));
        if (withKey(newKey, () => decryptSecret(fresh)) !== p.secret) die('round-trip check failed; nothing changed');
        const res = await client.query(
          'UPDATE admin_security SET totp_secret_cipher = $3 WHERE user_id = $1 AND totp_secret_cipher = $2',
          [p.user_id, p.old, fresh]);
        if (res.rowCount !== 1) die('a row changed while re-encrypting; nothing changed');
        p.new = fresh;
      }
      // Verify every row from the database, with the NEW key, before commit.
      const after = (await client.query('SELECT user_id, totp_secret_cipher FROM admin_security ORDER BY user_id')).rows;
      const byUser = new Map(after.map(r => [r.user_id, r.totp_secret_cipher]));
      for (const p of plain) {
        const stored = byUser.get(p.user_id);
        if (stored === undefined || stored !== p.new) die('post-check: stored value differs; nothing changed');
        if (withKey(newKey, () => decryptSecret(stored)) !== p.secret) die('post-check: row does not decrypt with NEW key; nothing changed');
      }
      await client.query('COMMIT');
      console.log(`COMMITTED: re-encrypted ${plain.length} row(s). Set PHASE_B_ADMIN_KEY to the NEW key in the env file and restart the API.`);
    }
  } catch (e) {
    try { await client.query('ROLLBACK'); } catch (_) {}
    console.error('STOPPED:', e.message.split('\n')[0]);
    process.exitCode = 1;
  } finally {
    plain.forEach(p => { p.secret = null; });
    client.release();
    await pool.end();
  }
}
main().catch(e => { console.error('STOPPED:', e.message.split('\n')[0]); process.exit(1); });
