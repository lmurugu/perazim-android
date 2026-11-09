'use strict';

const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { args, readPrivatePinFile, writeEnrollmentFile } = require('../scripts/bootstrap-synthetic-admin');

const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-admin-enrollment-'));
try {
  assert.throws(() => args(['--recover', '--new-pin', '12345678']), /PIN values are not accepted/);
  const interactive = args(['--bootstrap', '--create-account', '--db', 'perazim_phaseb_abcdef_test',
    '--confirm', 'perazim_phaseb_abcdef_test', '--approval', 'PILOT_TEST_APPROVAL']);
  assert.strictEqual(interactive.mode, 'bootstrap');
  assert.strictEqual(interactive.createAccount, true);
  assert.strictEqual(interactive.db, 'perazim_phaseb_abcdef_test');
  const enrollment = path.join(dir, 'enrollment.json');
  writeEnrollmentFile(enrollment, { totp_secret: 'SYNTHETIC_ONLY_TEST_SECRET' });
  assert.strictEqual(fs.statSync(enrollment).mode & 0o777, 0o600);
  assert.strictEqual(JSON.parse(fs.readFileSync(enrollment, 'utf8')).totp_secret, 'SYNTHETIC_ONLY_TEST_SECRET');
  assert.throws(() => writeEnrollmentFile(enrollment, { totp_secret: 'replacement' }), /EEXIST/);

  const pinFile = path.join(dir, 'pin.txt');
  fs.writeFileSync(pinFile, '654321\n', { mode: 0o600 });
  assert.strictEqual(readPrivatePinFile(pinFile), '654321');
  fs.chmodSync(pinFile, 0o644);
  assert.throws(() => readPrivatePinFile(pinFile), /owner/);
  process.stdout.write('PASS: enrollment files are owner-only, exclusive-create, and PINs are not accepted as CLI values.\n');
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}
