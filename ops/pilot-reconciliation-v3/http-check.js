// Scratch-only behaviour check against the real privacy router and the gate's exact SQL.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { CLONE, SCRATCH_PG } = require('./scratch-env');
const dbName = process.argv[2];
process.env.DATABASE_URL = SCRATCH_PG + '/' + dbName;
process.env.NODE_ENV = process.env.CHECK_ENV || 'production';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'scratch-only-' + crypto.randomBytes(16).toString('hex');
const testText = 'SCRATCH TEST NOTICE - not approved\n';
const sha = s => crypto.createHash('sha256').update(s).digest('hex');
const express = require(CLONE + '/node_modules/express');
const db = require(CLONE + '/src/db.js');
const privacyRoutes = require(CLONE + '/src/routes/privacy.js');

const GATE_SQL = `SELECT version,content_hash FROM privacy_notice_versions
        WHERE is_active=TRUE AND publication_status='PUBLISHED' AND withdrawn_at IS NULL AND effective_at <= $1 LIMIT 1`;

(async () => {
  await db.initDb();
  const app = express();
  app.use('/api/privacy', privacyRoutes);
  const server = app.listen(0, '127.0.0.1');
  await new Promise(r => server.once('listening', r));
  const port = server.address().port;
  const q = (s, p) => db.query(s, p);
  const setState = async (activeVersion) => {
    await q('UPDATE privacy_notice_versions SET is_active = FALSE WHERE is_active');
    if (activeVersion) await q('UPDATE privacy_notice_versions SET is_active = TRUE WHERE version = $1', [activeVersion]);
  };
  const check = async (label, activeVersion) => {
    await setState(activeVersion);
    const r = await fetch(`http://127.0.0.1:${port}/api/privacy/notice/current`);
    const body = await r.text();
    const gate = (await q(GATE_SQL, [Date.now()])).rows;
    const leaked = body.includes('SCRATCH TEST NOTICE');
    console.log(`${label}: /notice/current HTTP ${r.status}; test text served=${leaked}; gate rows=${gate.length}${gate.length ? ' (' + gate[0].version + ')' : ''}`);
  };
  // Pre-reconciliation: 0.1-TEST is the active PUBLISHED row. Its file must exist for the hash check.
  fs.writeFileSync(path.join(CLONE, 'test-privacy-notice.md'), testText);
  try {
    await check('PRE  (0.1-TEST active, PUBLISHED)', '0.1-TEST');
    await check('RECON (nothing active)            ', null);
    await check('POST (0.2 DRAFT active, unmodified 0023)', '0.2');
    await check('ALT  (0.2 inactive, nothing active)', null);
  } finally {
    fs.unlinkSync(path.join(CLONE, 'test-privacy-notice.md'));
    server.close(); await db.closeDb();
  }
})().catch(e => { console.error('ERROR', e.stack || e.message); process.exit(1); });
