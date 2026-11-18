'use strict';

const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { createOfflinePackage, verifyOfflinePackage, createRollbackAuthorization,
  verifyRollbackAuthorization, createWithdrawalManifest, verifyWithdrawalManifest } = require('../src/phase-b/offlineKnowledgePackage');

const keys = crypto.generateKeyPairSync('ed25519');
const trustedKeys = { 'synthetic-key-v1': keys.publicKey };
const text = 'Synthetic approved teaching passage for package tests.';
const source = {
  source_id: 'synthetic-source', version: '1', title: 'Synthetic teaching',
  authority_class: 'APPROVED_TEACHING', approval_status: 'APPROVED',
  publication_status: 'PUBLISHED', confidentiality: 'PUBLIC',
  permitted_audiences: ['PUBLIC'], permitted_uses: ['OFFLINE'],
  approval_authority: 'Synthetic reviewer', approval_reference: 'FIXTURE-1',
  approved_at: 1, location: 'section 1', language: 'en', text,
  content_hash: crypto.createHash('sha256').update(text).digest('hex')
};
const now = 1_800_000_000_000;
const canonical = value => Array.isArray(value) ? `[${value.map(canonical).join(',')}]`
  : value && typeof value === 'object'
    ? `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${canonical(value[key])}`).join(',')}}`
    : JSON.stringify(value);
const envelope = createOfflinePackage({
  packageId: 'synthetic-public', version: '1.0.0', publishedAt: now - 1000,
  expiresAt: now + 60_000, minimumAppVersion: '2.2.0',
  signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey, sources: [source]
});
assert.equal(verifyOfflinePackage(envelope, { trustedKeys, now, appVersion: '2.2.0' }).documents[0].text, text);
const unsignedExpiry = structuredClone(envelope);
unsignedExpiry.payload.expiresAt = null;
delete unsignedExpiry.payload.contentDigest;
const unsignedPayload = { ...unsignedExpiry.payload };
unsignedExpiry.payload.contentDigest = crypto.createHash('sha256').update(canonical(unsignedPayload)).digest('hex');
unsignedExpiry.signature = crypto.sign(null, Buffer.from(canonical(unsignedExpiry.payload)), keys.privateKey).toString('base64');
assert.throws(() => verifyOfflinePackage(unsignedExpiry, { trustedKeys, now, appVersion: '2.2.0' }),
  { code: 'PACKAGE_INVALID' }, 'a correctly signed package still requires a finite expiry');
assert.throws(() => createOfflinePackage({
  packageId: 'missing-expiry', version: '1', publishedAt: now,
  minimumAppVersion: '2.2.0', signingKeyId: 'synthetic-key-v1',
  privateKey: keys.privateKey, sources: [source]
}), { code: 'INVALID_PACKAGE' });
assert.throws(() => createOfflinePackage({
  packageId: 'missing-language', version: '2', publishedAt: now,
  expiresAt: now + 60_000,
  minimumAppVersion: '2.2.0', signingKeyId: 'synthetic-key-v1',
  privateKey: keys.privateKey, sources: [{ ...source, language: undefined }]
}), { code: 'SOURCE_NOT_ELIGIBLE' });
assert.throws(() => createOfflinePackage({
  packageId: 'empty-not-authorized', version: '2', publishedAt: now,
  minimumAppVersion: '2.2.0', signingKeyId: 'synthetic-key-v1',
  privateKey: keys.privateKey, sources: []
}), { code: 'INVALID_PACKAGE' });
const emptySnapshot = createOfflinePackage({
  packageId: 'synthetic-public', version: '2', publishedAt: now,
  expiresAt: now + 60_000, minimumAppVersion: '2.2.0',
  signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  sources: [], allowEmptySnapshot: true
});
const verifiedEmptySnapshot = verifyOfflinePackage(emptySnapshot, { trustedKeys, now, appVersion: '2.2.0' });
assert.equal(verifiedEmptySnapshot.snapshotKind, 'EMPTY');
assert.equal(verifiedEmptySnapshot.documents.length, 0);
assert.throws(() => verifyOfflinePackage({ ...envelope, signature: Buffer.alloc(64).toString('base64') },
  { trustedKeys, now, appVersion: '2.2.0' }), { code: 'PACKAGE_INVALID' });
assert.throws(() => verifyOfflinePackage(envelope, { trustedKeys, now: now + 60_000, appVersion: '2.2.0' }),
  { code: 'PACKAGE_INVALID' });
assert.throws(() => verifyOfflinePackage(envelope, { trustedKeys, now, appVersion: '2.1.9' }),
  { code: 'PACKAGE_INVALID' });
assert.throws(() => createOfflinePackage({
  packageId: 'bad', version: '1', publishedAt: now, minimumAppVersion: '1',
  expiresAt: now + 60_000,
  signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  sources: [{ ...source, confidentiality: 'RESTRICTED' }]
}), { code: 'SOURCE_NOT_ELIGIBLE' });
assert.throws(() => createOfflinePackage({
  packageId: 'bad-expiry', version: '1', publishedAt: now, expiresAt: now,
  minimumAppVersion: '1', signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  sources: [source]
}), { code: 'INVALID_PACKAGE' });
const nearLimitText = 'x'.repeat(2 * 1024 * 1024 - 750);
assert.throws(() => createOfflinePackage({
  packageId: 'oversized-envelope', version: '1', publishedAt: now,
  expiresAt: now + 60_000,
  minimumAppVersion: '1', signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  sources: [{ ...source, text: nearLimitText,
    content_hash: crypto.createHash('sha256').update(nearLimitText).digest('hex') }]
}), error => error.code === 'PACKAGE_TOO_LARGE' && /envelope/.test(error.message));
const tampered = structuredClone(envelope);
tampered.payload.documents[0].text += ' altered';
assert.throws(() => verifyOfflinePackage(tampered, { trustedKeys, now, appVersion: '2.2.0' }),
  { code: 'PACKAGE_INVALID' });
const rollback = createRollbackAuthorization({ controlSequence: 1, issuedAt: now - 500,
  expiresAt: now + 30_000, targetPackageId: 'synthetic-public', targetDigest: envelope.payload.contentDigest,
  reasonCode: 'PACKAGE_ROLLBACK', signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey });
assert.equal(verifyRollbackAuthorization(rollback, { trustedKeys, now }).targetDigest,
  envelope.payload.contentDigest, 'a valid signed rollback names its exact package digest');
assert.throws(() => verifyRollbackAuthorization(rollback, { trustedKeys, now: now + 30_000 }),
  { code: 'ROLLBACK_INVALID' }, 'expired rollback controls are rejected');
const tamperedRollback = structuredClone(rollback);
tamperedRollback.payload.targetDigest = 'f'.repeat(64);
assert.throws(() => verifyRollbackAuthorization(tamperedRollback, { trustedKeys, now }),
  { code: 'ROLLBACK_INVALID' }, 'a changed rollback target is rejected');
assert.throws(() => createRollbackAuthorization({ controlSequence: 0, issuedAt: now,
  expiresAt: now + 30_000, targetPackageId: 'synthetic-public', targetDigest: envelope.payload.contentDigest,
  reasonCode: 'PACKAGE_ROLLBACK', signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey }),
{ code: 'INVALID_ROLLBACK_AUTHORIZATION' }, 'rollback control sequences must be positive');
const withdrawal = createWithdrawalManifest({ controlSequence: 2, issuedAt: now - 500,
  expiresAt: now + 30_000, signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  withdrawnSources: [{ sourceId: 'synthetic-source', version: 'v1', reasonCode: 'SOURCE_ERROR' }] });
assert.deepEqual(verifyWithdrawalManifest(withdrawal, { trustedKeys, now }).withdrawnSources,
  [{ sourceId: 'synthetic-source', version: 'v1', reasonCode: 'SOURCE_ERROR' }]);
assert.throws(() => verifyWithdrawalManifest(withdrawal, { trustedKeys, now: now + 30_000 }),
  { code: 'CONTROL_INVALID' }, 'expired withdrawal controls are rejected');
const tamperedWithdrawal = structuredClone(withdrawal);
tamperedWithdrawal.payload.withdrawnSources[0].version = 'v2';
assert.throws(() => verifyWithdrawalManifest(tamperedWithdrawal, { trustedKeys, now }),
  { code: 'CONTROL_INVALID' }, 'withdrawal source tampering is rejected');
assert.throws(() => createWithdrawalManifest({ controlSequence: 3, issuedAt: now,
  expiresAt: now + 30_000, signingKeyId: 'synthetic-key-v1', privateKey: keys.privateKey,
  withdrawnSources: [{ sourceId: 'synthetic-source', version: 'v1', reasonCode: 'SOURCE_ERROR' },
    { sourceId: 'synthetic-source', version: 'v1', reasonCode: 'REPLACED' }] }),
{ code: 'INVALID_CONTROL_MANIFEST' }, 'duplicate withdrawn source versions are rejected');
process.stdout.write('PASS: synthetic public package signing, trust, expiry, version, and content integrity.\n');
