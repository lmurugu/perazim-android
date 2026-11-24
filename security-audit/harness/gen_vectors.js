// Synthetic-only vector generator for the Android Ed25519Verifier and canonical JSON.
// No real keys: keys are generated fresh in this process and never stored.
'use strict';
const crypto = require('crypto');
const fs = require('fs');

// Copy of server/src/phase-b/offlineKnowledgePackage.js canonical() (verbatim logic).
function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}';
  }
  return JSON.stringify(value);
}
const L = 2n ** 252n + 27742317777372353535851937790883648493n;
const toLE = (buf) => BigInt('0x' + Buffer.from(buf).reverse().toString('hex'));
const fromLE = (n) => { const b = Buffer.alloc(32); let x = n; for (let i = 0; i < 32; i++) { b[i] = Number(x & 0xffn); x >>= 8n; } return b; };

const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
const pk = publicKey.export({ format: 'der', type: 'spki' }).subarray(12);
const vectors = [];
const add = (label, pkBytes, msg, sig, expected) => vectors.push({
  label, pk: pkBytes.toString('base64'), msg: Buffer.from(msg).toString('base64'),
  sig: sig.toString('base64'), expected });

const messages = [
  'ascii package payload {"a":1}',
  'unicode ’ — é \u0085 payload',
  '</script> slash-after-lt',
  '{"documents":[{"text":"Peace – grace"}]}',
  '',
];
for (const [i, m] of messages.entries()) {
  const sig = crypto.sign(null, Buffer.from(m, 'utf8'), privateKey);
  add('valid-' + i, pk, Buffer.from(m, 'utf8'), sig, true);
  const flipped = Buffer.concat([Buffer.from(m, 'utf8'), Buffer.from([0x21])]);
  add('tampered-msg-' + i, pk, flipped, sig, false);
  const badSig = Buffer.from(sig); badSig[10] ^= 0x40;
  add('tampered-sig-' + i, pk, Buffer.from(m, 'utf8'), badSig, false);
  // Malleable s' = s + L (must be rejected: non-canonical S).
  const s = toLE(sig.subarray(32, 64));
  if (s + L < 2n ** 256n) {
    const malleated = Buffer.concat([sig.subarray(0, 32), fromLE(s + L)]);
    add('malleated-S-plus-L-' + i, pk, Buffer.from(m, 'utf8'), malleated, false);
  }
}
// Other trusted key must not verify.
const other = crypto.generateKeyPairSync('ed25519');
const otherPk = other.publicKey.export({ format: 'der', type: 'spki' }).subarray(12);
add('wrong-key', otherPk, Buffer.from(messages[0]), crypto.sign(null, Buffer.from(messages[0]), privateKey), false);
// Identity public key with R = identity and S = 0 (probe: small-order key acceptance).
const identityPk = Buffer.from('01' + '00'.repeat(31), 'hex');
const identityR = Buffer.from('01' + '00'.repeat(31), 'hex');
add('probe-identity-key-forgery', identityPk, Buffer.from('any message'), Buffer.concat([identityR, Buffer.alloc(32)]), false);

// Canonical JSON interop: Node output for payloads containing characters Android org.json escapes.
const payloads = [
  { text: 'Peace ’ grace — hope', version: '1.0' },
  { text: '\u0085 nel', version: '1' },
  { text: 'a</b>', version: '2' },
  { text: 'plain ascii', version: '3' },
];
const canonicalCases = payloads.map(p => ({ node: canonical(p) }));

fs.writeFileSync(process.argv[2] || 'vectors.json', JSON.stringify({ vectors, canonicalCases }, null, 1));
console.log('vectors', vectors.length, 'canonical cases', canonicalCases.length);

// Bulk regression: 200 fresh keys, random payload-like messages, valid signatures must verify.
const bulk = [];
for (let i = 0; i < 200; i++) {
  const kp = crypto.generateKeyPairSync('ed25519');
  const raw = kp.publicKey.export({ format: 'der', type: 'spki' }).subarray(12);
  const msg = Buffer.from(JSON.stringify({ n: i, t: 'synthetic ’ text ' + i }), 'utf8');
  add('bulk-valid-' + i, raw, msg, crypto.sign(null, msg, kp.privateKey), true);
}
const out = JSON.parse(fs.readFileSync(process.argv[2] || 'vectors.json', 'utf8'));
out.vectors = out.vectors.concat(vectors.slice(-200));
fs.writeFileSync(process.argv[2] || 'vectors.json', JSON.stringify(out, null, 1));
console.log('total vectors', out.vectors.length);
