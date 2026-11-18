'use strict';

const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { args, readPrivatePinFile, writeEnrollmentFile, promptPinPair } = require('../scripts/bootstrap-synthetic-admin');

async function main() {
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'perazim-admin-enrollment-'));
try {
  assert.throws(() => args(['--recover', '--new-pin', '12345678']), /PIN values are not accepted/);
  const interactive = args(['--bootstrap', '--create-account', '--db', 'perazim_phaseb_abcdef_test',
    '--confirm', 'perazim_phaseb_abcdef_test', '--approval', 'PILOT_TEST_APPROVAL']);
  assert.strictEqual(interactive.mode, 'bootstrap');
  assert.strictEqual(interactive.createAccount, true);
  assert.strictEqual(interactive.db, 'perazim_phaseb_abcdef_test');
  const recovery = args(['--recover', '--recover-interactive', '--db', 'perazim_phaseb_abcdef_test',
    '--confirm', 'perazim_phaseb_abcdef_test', '--account', 'synthetic_pilot_abcdef12', '--approval', 'LEVI-RECOVERY-TEST']);
  assert.strictEqual(recovery.mode, 'recover');
  assert.strictEqual(recovery.recoverInteractive, true);
  assert.strictEqual(Object.hasOwn(recovery, 'pinFile'), false, 'interactive recovery does not require a PIN file');

  const enteredPins = ['65432178', '65432178'];
  assert.strictEqual(await promptPinPair(async () => enteredPins.shift()), '65432178');
  await assert.rejects(promptPinPair(async () => '1234'), /6–12 digits/);
  const mismatchedPins = ['65432178', '87654321'];
  await assert.rejects(promptPinPair(async () => mismatchedPins.shift()), /did not match/);
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
  process.stdout.write('PASS: owner-only TOTP files, hidden 6–12 digit PIN pairing, interactive recovery flags, and no PIN CLI values.\n');
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}
}

main().catch(error => {
  process.stderr.write(`${error.name}: ${error.message}\n`);
  process.exitCode = 1;
});
