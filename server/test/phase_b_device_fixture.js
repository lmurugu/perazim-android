'use strict';

// Optional physical-device acceptance fixture. Owns only its new test DB and
// loopback server. Pass its private output file to the instrumentation driver.
const fs = require('fs');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const { migrate } = require('../src/migrations');

async function main() {
  if (process.env.NODE_ENV === 'production') throw new Error('Synthetic fixture only');
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  if (adminUrl.pathname !== '/postgres' || !['127.0.0.1', 'localhost', '[::1]'].includes(adminUrl.hostname)) throw new Error('Local test maintenance database required');
  const output = process.env.PHASE_B_DEVICE_FIXTURE_FILE || '';
  if (!/^\/tmp\/perazim-phaseb-device-[a-f0-9]+\.json$/.test(output)) throw new Error('A unique /tmp Phase B fixture filename is required');
  const suffix = crypto.randomBytes(8).toString('hex');
  const database = `perazim_phaseb_${suffix}_test`;
  const pilot = `synthetic_pilot_${suffix}`;
  const member = `synthetic_member_${suffix}`;
  const admin = new Pool({ connectionString: adminUrl.toString() });
  const db = require('../src/db');
  let pool, server, ownsFile = false, created = false;
  try {
    await admin.query(`CREATE DATABASE "${database}"`);
    created = true;
    adminUrl.pathname = '/' + database;
    Object.assign(process.env, {
      NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true', DATABASE_URL: adminUrl.toString(),
      JWT_SECRET: crypto.randomBytes(48).toString('hex'), PHASE_B_ADMIN_KEY: crypto.randomBytes(32).toString('hex')
    });
    pool = new Pool({ connectionString: adminUrl.toString() });
    await migrate(pool);
    const now = Date.now();
    const pin = String(100000 + crypto.randomInt(900000));
    const pilotPhone = '+254700008861', memberPhone = '+254700008862';
    const memberName = 'Synthetic Device Claim';
    const pinHash = await bcrypt.hash(pin, 10);
    await pool.query(`INSERT INTO users(id,phone,pin_hash,name,role,is_active,created_at,updated_at)
      VALUES($1,$2,$3,'Synthetic Device Pilot','MEMBER',TRUE,$4,$4),
      ($5,$6,$3,$7,'MEMBER',TRUE,$4,$4)`, [pilot, pilotPhone, pinHash, now, member, memberPhone, memberName]);
    const { bootstrap } = require('../scripts/bootstrap-synthetic-admin');
    const enrollment = await bootstrap(pool, { db: database, confirm: database, account: pilot, approval: 'SYNTHETIC_DEVICE_TEST' });
    await db.initDb();
    const { app } = require('../src/index');
    server = await new Promise((resolve, reject) => {
      const listener = app.listen(55476, '127.0.0.1', () => resolve(listener));
      listener.once('error', reject);
    });
    fs.writeFileSync(output, JSON.stringify({ pin, pilotPhone, memberPhone, memberName, totpSecret: enrollment.totp_secret }), { mode: 0o600, flag: 'wx' });
    ownsFile = true;
    console.log(`Synthetic device fixture listening on loopback 55476; database ${database}.`);
    await new Promise(resolve => {
      const timeout = setTimeout(resolve, 15 * 60 * 1000);
      const stop = () => { clearTimeout(timeout); resolve(); };
      process.once('SIGTERM', stop);
      process.once('SIGINT', stop);
    });
  } finally {
    if (ownsFile) fs.unlinkSync(output);
    if (server) await new Promise(resolve => server.close(resolve));
    await db.closeDb();
    if (pool) await pool.end();
    try {
      if (created) {
        for (let attempt = 0; ; attempt++) {
          try { await admin.query(`DROP DATABASE "${database}"`); break; }
          catch (error) {
            if (error.code !== '55006' || attempt === 4) throw error;
            await new Promise(resolve => setTimeout(resolve, 150));
          }
        }
      }
    } finally { await admin.end(); }
  }
}

main().catch(error => { console.error('Synthetic device fixture failed:', error.message); process.exitCode = 1; });
