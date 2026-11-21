'use strict';

// Approver signatures for institutional source approvals (S2-06 item 3). Must match the device's SourceApprovals.java:
// domain prefix, signed field set, nulls for absent fields, canonical JSON with sorted keys.
const crypto = require('node:crypto');

const DOMAIN = 'perazim-source-approval-v1\n';
const CHURCH_APPROVED = 'CHURCH_APPROVED';
const SIGNED_FIELDS = ['id', 'version', 'category', 'approvedBy', 'approvedAt', 'scope', 'sha256'];
const SPKI_ED25519_PREFIX = Buffer.from('302a300506032b6570032100', 'hex');
const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

function canonical(value) {
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`;
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${canonical(value[key])}`).join(',')}}`;
  }
  return JSON.stringify(value);
}

function approvalMessage(entry) {
  const signed = {};
  for (const field of SIGNED_FIELDS) signed[field] = entry[field] ?? null;
  return Buffer.from(DOMAIN + canonical(signed), 'utf8');
}

// Reads PERAZIM_SOURCE_APPROVER_KEYS: a JSON object of keyId -> base64 raw 32-byte Ed25519 public key.
// Unset means no approver keys, so no source can be approved or packaged as official (fail closed).
function approverPublicKeys(env = process.env) {
  const raw = env.PERAZIM_SOURCE_APPROVER_KEYS;
  if (!raw) return Object.freeze({});
  const parsed = JSON.parse(raw);
  const keys = {};
  for (const [keyId, encoded] of Object.entries(parsed)) {
    if (!/^[A-Za-z0-9._-]{1,80}$/.test(keyId) || typeof encoded !== 'string' || !BASE64.test(encoded)) {
      throw new Error('Approver key configuration is malformed.');
    }
    const raw32 = Buffer.from(encoded, 'base64');
    if (raw32.length !== 32 || raw32.toString('base64') !== encoded) throw new Error('Approver key configuration is malformed.');
    keys[keyId] = crypto.createPublicKey({ key: Buffer.concat([SPKI_ED25519_PREFIX, raw32]), format: 'der', type: 'spki' });
  }
  return Object.freeze(keys);
}

function verifyApprovalSignature(entry, approverKeys) {
  if (!entry || typeof entry.approvedBy !== 'string' || typeof entry.approvalSignature !== 'string') return false;
  if (!Object.prototype.hasOwnProperty.call(approverKeys || {}, entry.approvedBy)) return false;
  if (!BASE64.test(entry.approvalSignature)) return false;
  const signature = Buffer.from(entry.approvalSignature, 'base64');
  if (signature.length !== 64 || signature.toString('base64') !== entry.approvalSignature) return false;
  return crypto.verify(null, approvalMessage(entry), approverKeys[entry.approvedBy], signature);
}

// Used by the approver's offline signing tool and by tests. The private key never reaches the server.
function signApproval(entry, privateKey) {
  return crypto.sign(null, approvalMessage(entry), privateKey).toString('base64');
}

module.exports = { CHURCH_APPROVED, DOMAIN, approvalMessage, approverPublicKeys, signApproval, verifyApprovalSignature };
