// Synthetic-only: fresh Ed25519 keys per run. Builds governance entries signed the way the offline approver would.
'use strict';
const crypto = require('crypto');
const fs = require('fs');
const canonical = (value) => {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') return '{' + Object.keys(value).sort().map(k => JSON.stringify(k) + ':' + canonical(value[k])).join(',') + '}';
  return JSON.stringify(value);
};
const FIELDS = ['id', 'version', 'category', 'approvedBy', 'approvedAt', 'scope', 'sha256'];
const DOMAIN = 'perazim-source-approval-v1\n';
const mk = () => { const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
  return { raw: publicKey.export({ format: 'der', type: 'spki' }).subarray(12).toString('base64'), privateKey }; };
const approver = mk(), approver2 = mk(), pkg = mk();
const trust = {
  'approver-2026-a': 'APPROVER:' + approver.raw,
  'approver-2026-b': 'APPROVER:' + approver2.raw,
  'synthetic-device-key': 'PACKAGE:' + pkg.raw,
  'legacy-raw-approver': approver.raw,           // raw value, no purpose: must be refused
};
const base = (over) => Object.assign({ id: 'syn-source-1', version: '1.0', category: 'CHURCH_APPROVED',
  approvedBy: 'approver-2026-a', approvedAt: 1767000000000, scope: 'OFFLINE:CHAT', sha256: 'a'.repeat(64) }, over);
const sign = (entry, key, domain = DOMAIN) => {
  const signed = {}; for (const f of FIELDS) signed[f] = entry[f] === undefined ? null : entry[f];
  return crypto.sign(null, Buffer.from(domain + canonical(signed), 'utf8'), key.privateKey).toString('base64');
};
const cases = [];
const add = (label, entry, expected) => cases.push({ label, entry, expected });
let e = base({}); add('approved-by-approver', Object.assign(e, { approvalSignature: sign(e, approver) }), true);
e = base({ scope: 'OFFLINE:’ peace — grace </x>' }); add('unicode-scope-signed', Object.assign(e, { approvalSignature: sign(e, approver) }), true);
e = base({ category: 'REFERENCE' }); add('reference-no-signature-ok', e, true);
e = base({ category: 'REFERENCE' }); add('reference-with-valid-signature', Object.assign(e, { approvalSignature: sign(e, approver) }), true);
e = base({}); add('church-approved-no-signature', e, false);
e = base({}); add('signed-by-package-key-as-approver', Object.assign(e, { approvedBy: 'synthetic-device-key', approvalSignature: sign(e, pkg) }), false);
e = base({}); add('approvedBy-package-key-with-its-sig', Object.assign(e, { approvedBy: 'synthetic-device-key', approvalSignature: sign(e, pkg) }), false);
e = base({}); const sig = sign(e, approver); add('tampered-sha256', Object.assign(base({ sha256: 'b'.repeat(64) }), { approvalSignature: sig }), false);
add('tampered-approvedAt', Object.assign(base({ approvedAt: 1767000000001 }), { approvalSignature: sig }), false);
add('tampered-scope', Object.assign(base({ scope: 'OFFLINE:ALL' }), { approvalSignature: sig }), false);
add('approver-b-but-signed-by-a', Object.assign(base({ approvedBy: 'approver-2026-b' }), { approvalSignature: sig }), false);
add('unknown-approver-id', Object.assign(base({ approvedBy: 'nobody' }), { approvalSignature: sig }), false);
add('legacy-raw-value-approver', Object.assign(base({ approvedBy: 'legacy-raw-approver' }), { approvalSignature: sign(base({ approvedBy: 'legacy-raw-approver' }), approver) }), false);
e = base({}); add('signature-without-domain-prefix', Object.assign(e, { approvalSignature: sign(e, approver, '') }), false);
add('malformed-signature-length', Object.assign(base({}), { approvalSignature: Buffer.alloc(10).toString('base64') }), false);
fs.writeFileSync('approval_cases.json', JSON.stringify({ trust, cases }, null, 1));
console.log('cases', cases.length);
