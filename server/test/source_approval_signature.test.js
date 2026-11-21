'use strict';

// S2-06 item 3, server side. Synthetic keys only. The approver key lives only in this test process.
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { createOfflinePackage, verifyOfflinePackage } = require('../src/phase-b/offlineKnowledgePackage');
const { approverPublicKeys, signApproval, verifyApprovalSignature } = require('../src/phase-b/sourceApprovalSignature');

const approver = crypto.generateKeyPairSync('ed25519');
const packageKey = crypto.generateKeyPairSync('ed25519');
const rawOf = key => key.export({ format: 'der', type: 'spki' }).subarray(12).toString('base64');
const env = { PERAZIM_SOURCE_APPROVER_KEYS: JSON.stringify({ 'approver-synthetic': rawOf(approver.publicKey) }) };
const approverKeys = approverPublicKeys(env);
const pkgTrust = { 'synthetic-key-v1': packageKey.publicKey };
const now = 1_800_000_000_000;
const text = 'Synthetic official passage for approval-signature tests. ’ dashes — here.';
const contentHash = crypto.createHash('sha256').update(text).digest('hex');

function officialSource(overrides = {}) {
  return Object.assign({
    source_id: 'synthetic-official', version: '1', title: 'Synthetic official', authority_class: 'APPROVED_OFFICIAL',
    approval_status: 'APPROVED', publication_status: 'PUBLISHED', confidentiality: 'PUBLIC',
    permitted_audiences: ['PUBLIC'], permitted_uses: ['OFFLINE'],
    approval_authority: 'approver-synthetic', approval_reference: 'SYNTH-REF-1',
    approved_at: now - 1000, location: 'section 1', language: 'en', text, content_hash: contentHash
  }, overrides);
}
// What the approver signs offline, over exactly these fields.
function approvedEntry(source, approvedAt = source.approved_at) {
  return { id: source.source_id, version: source.version, category: 'CHURCH_APPROVED',
    approvedBy: source.approval_authority, approvedAt, scope: 'OFFLINE:' + source.permitted_uses.join(','),
    sha256: source.content_hash };
}
const signed = source => signApproval(approvedEntry(source), approver.privateKey);
const pack = (sources, opts = {}) => createOfflinePackage({ packageId: 'synthetic-pkg-1', version: '1.0.0',
  publishedAt: now - 2000, expiresAt: now + 86_400_000, minimumAppVersion: '1.0.0', signingKeyId: 'synthetic-key-v1',
  privateKey: packageKey.privateKey, sources, approverKeys, ...opts });

// 1. Signed official source: package includes the approval signature and the device-side verifier accepts it.
{
  const source = officialSource({ approval_signature: signed(officialSource()) });
  const envelope = pack([source]);
  const entry = envelope.payload.governance[0];
  assert.equal(entry.category, 'CHURCH_APPROVED');
  assert.equal(entry.approvalSignature, source.approval_signature);
  assert.ok(verifyOfflinePackage(envelope, { trustedKeys: pkgTrust, now, appVersion: '1.0.0' }));
}

// 2. Official source without a signature is refused at packaging.
assert.throws(() => pack([officialSource()]), /not eligible|approved, public, intact|valid approver signature/);

// 3. Signature from a key that is not an approver is refused.
{
  const stranger = crypto.generateKeyPairSync('ed25519');
  const source = officialSource({ approval_signature: signApproval(approvedEntry(officialSource()), stranger.privateKey) });
  assert.throws(() => pack([source]), /valid approver signature/);
}

// 4. Changing the signed approval time after signing breaks the signature.
{
  const source = officialSource({ approval_signature: signed(officialSource()), approved_at: now - 500 });
  assert.throws(() => pack([source]), /valid approver signature/);
}

// 5. Changing the content hash after signing breaks the signature.
{
  const source = officialSource({ approval_signature: signed(officialSource()), content_hash: 'c'.repeat(64) });
  assert.throws(() => pack([source]), /not eligible|intact|valid approver signature/);
}

// 6. A non-official source needs no approver signature and is not packaged as church-approved.
{
  const source = officialSource({ authority_class: 'APPROVED_TEACHING' });
  const entry = pack([source]).payload.governance[0];
  assert.equal(entry.category, 'UNAPPROVED');
  assert.equal(entry.approvalSignature, undefined);
}

// 7. With no approver keys configured, no official source can be packaged (fail closed).
{
  const source = officialSource({ approval_signature: signed(officialSource()) });
  assert.throws(() => pack([source], { approverKeys: approverPublicKeys({}) }), /valid approver signature/);
}

// 8. Malformed approver configuration is rejected at load time.
assert.throws(() => approverPublicKeys({ PERAZIM_SOURCE_APPROVER_KEYS: '{"bad id!":"AAAA"}' }));
assert.throws(() => approverPublicKeys({ PERAZIM_SOURCE_APPROVER_KEYS: JSON.stringify({ a: 'AAAA' }) }));

// 9. Prototype names are not approvers.
assert.equal(verifyApprovalSignature({ approvedBy: 'constructor', approvalSignature: 'AAAA' }, approverKeys), false);

console.log('source approval signature tests passed');
