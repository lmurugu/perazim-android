'use strict';

const crypto = require('node:crypto');
const { KnowledgeError } = require('./governedKnowledge');

const FORMAT = 'perazim-knowledge-v1';
const ROLLBACK_FORMAT = 'perazim-knowledge-rollback-v1';
const CONTROL_FORMAT = 'perazim-knowledge-control-v1';
const MAX_PACKAGE_BYTES = 2 * 1024 * 1024;
const ROLLBACK_REASONS = new Set(['PACKAGE_ROLLBACK', 'SOURCE_CORRECTION', 'SECURITY_REMEDIATION']);
const WITHDRAWAL_REASONS = new Set(['SOURCE_ERROR', 'AUTHORITY_WITHDRAWAL', 'LICENSING_CHANGE', 'REPLACED', 'SECURITY_REVIEW']);

function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}';
  }
  return JSON.stringify(value);
}

function createOfflinePackage({ packageId, version, publishedAt, expiresAt,
  minimumAppVersion, signingKeyId, privateKey, sources, allowEmptySnapshot = false }) {
  if (!packageId || !version || !Number.isSafeInteger(publishedAt) || !minimumAppVersion ||
      !signingKeyId || !privateKey || !Array.isArray(sources) ||
      (!sources.length && allowEmptySnapshot !== true)) {
    throw new KnowledgeError('INVALID_PACKAGE', 'Complete package metadata and signing key are required.');
  }
  if (!Number.isSafeInteger(expiresAt) || expiresAt <= publishedAt) {
    throw new KnowledgeError('INVALID_PACKAGE', 'Package expiry must be later than publication.');
  }
  const documents = sources.map(source => {
    if (!source || source.approval_status !== 'APPROVED' || source.publication_status !== 'PUBLISHED' ||
        source.confidentiality !== 'PUBLIC' || !source.permitted_audiences?.includes('PUBLIC') ||
        !source.permitted_uses?.includes('OFFLINE') || !source.approval_authority ||
        !source.approval_reference || !Number.isSafeInteger(Number(source.approved_at)) || Number(source.approved_at) <= 0 ||
        ['source_id', 'version', 'title', 'authority_class', 'location', 'language', 'text']
          .some(field => typeof source[field] !== 'string' || !source[field].trim()) ||
        !/^[a-f0-9]{64}$/.test(source.content_hash) ||
        crypto.createHash('sha256').update(source.text, 'utf8').digest('hex') !== source.content_hash) {
      throw new KnowledgeError('SOURCE_NOT_ELIGIBLE', 'Every package source must be approved, public, intact, and authorized for offline use.');
    }
    return {
      sourceId: source.source_id,
      version: source.version,
      title: source.title,
      authorityClass: source.authority_class,
      approvalAuthority: source.approval_authority,
      approvalReference: source.approval_reference,
      approvedAt: Number(source.approved_at),
      location: source.location,
      language: source.language,
      license: source.license || null,
      contentHash: source.content_hash,
      text: source.text
    };
  });
  const payload = { format: FORMAT, packageId, version, publishedAt, issuedAt: publishedAt,
    expiresAt, issuer: 'Perazim AI knowledge service', minimumAppVersion,
    snapshotKind: documents.length ? 'FULL' : 'EMPTY',
    audience: 'PUBLIC', documents };
  payload.contentDigest = crypto.createHash('sha256').update(canonical(payload), 'utf8').digest('hex');
  const bytes = Buffer.from(canonical(payload), 'utf8');
  if (bytes.length > MAX_PACKAGE_BYTES) throw new KnowledgeError('PACKAGE_TOO_LARGE', 'Knowledge package exceeds the size limit.');
  const signature = crypto.sign(null, bytes, privateKey).toString('base64');
  const envelope = { payload, signingKeyId, signature };
  if (Buffer.byteLength(JSON.stringify(envelope), 'utf8') > MAX_PACKAGE_BYTES) {
    throw new KnowledgeError('PACKAGE_TOO_LARGE', 'Knowledge package envelope exceeds the size limit.');
  }
  return envelope;
}

function verifyOfflinePackage(envelope, { trustedKeys, now = Date.now(), appVersion }) {
  if (!envelope || typeof envelope !== 'object' || !envelope.payload || !envelope.signature ||
      !trustedKeys || !trustedKeys[envelope.signingKeyId]) throw new KnowledgeError('PACKAGE_UNTRUSTED', 'Package signing key is not trusted.');
  const payload = envelope.payload;
  const bytes = Buffer.from(canonical(payload), 'utf8');
  const { contentDigest, ...unsignedPayload } = payload;
  const expectedDigest = crypto.createHash('sha256').update(canonical(unsignedPayload), 'utf8').digest('hex');
  if (bytes.length > MAX_PACKAGE_BYTES || contentDigest !== expectedDigest ||
      payload.format !== FORMAT || payload.audience !== 'PUBLIC' ||
      !Array.isArray(payload.documents) ||
      (payload.documents.length === 0 && payload.snapshotKind !== 'EMPTY') ||
      (payload.documents.length > 0 && payload.snapshotKind === 'EMPTY') ||
      !Number.isSafeInteger(payload.publishedAt) || payload.publishedAt > now ||
      (!Number.isSafeInteger(payload.expiresAt) || payload.expiresAt <= now) ||
      !appVersion || compareVersions(appVersion, payload.minimumAppVersion) < 0 ||
      !crypto.verify(null, bytes, trustedKeys[envelope.signingKeyId], Buffer.from(envelope.signature, 'base64'))) {
    throw new KnowledgeError('PACKAGE_INVALID', 'Package signature, expiry, format, or application compatibility is invalid.');
  }
  for (const document of payload.documents) {
    if (!document || !/^[a-f0-9]{64}$/.test(document.contentHash) ||
        crypto.createHash('sha256').update(document.text || '', 'utf8').digest('hex') !== document.contentHash) {
      throw new KnowledgeError('PACKAGE_HASH_MISMATCH', 'A knowledge document failed integrity verification.');
    }
  }
  return payload;
}

function createRollbackAuthorization({ controlSequence, issuedAt, expiresAt, targetPackageId,
  targetDigest, reasonCode, signingKeyId, privateKey }) {
  if (!Number.isSafeInteger(controlSequence) || controlSequence <= 0 ||
      !Number.isSafeInteger(issuedAt) || issuedAt <= 0 ||
      !Number.isSafeInteger(expiresAt) || expiresAt <= issuedAt ||
      !/^[A-Za-z0-9._:-]{1,120}$/.test(targetPackageId || '') ||
      !/^[a-f0-9]{64}$/.test(targetDigest || '') || !ROLLBACK_REASONS.has(reasonCode) ||
      !/^[A-Za-z0-9._-]{1,80}$/.test(signingKeyId || '') || !privateKey) {
    throw new KnowledgeError('INVALID_ROLLBACK_AUTHORIZATION', 'A signed rollback requires an exact target, reason, and increasing control sequence.');
  }
  const payload = { format: ROLLBACK_FORMAT, controlSequence, issuedAt, expiresAt,
    targetPackageId, targetDigest, reasonCode };
  payload.controlDigest = crypto.createHash('sha256').update(canonical(payload), 'utf8').digest('hex');
  const signature = crypto.sign(null, Buffer.from(canonical(payload), 'utf8'), privateKey).toString('base64');
  return { payload, signingKeyId, signature };
}

function verifyRollbackAuthorization(envelope, { trustedKeys, now = Date.now() }) {
  if (!envelope || typeof envelope !== 'object' || !envelope.payload ||
      !/^[A-Za-z0-9._-]{1,80}$/.test(envelope.signingKeyId || '') ||
      !trustedKeys || !trustedKeys[envelope.signingKeyId] ||
      typeof envelope.signature !== 'string' || !/^[A-Za-z0-9+/]+={0,2}$/.test(envelope.signature)) {
    throw new KnowledgeError('ROLLBACK_UNTRUSTED', 'Rollback signing key is not trusted.');
  }
  const payload = envelope.payload;
  const { controlDigest, ...unsigned } = payload;
  const expectedDigest = crypto.createHash('sha256').update(canonical(unsigned), 'utf8').digest('hex');
  const signatureBytes = Buffer.from(envelope.signature, 'base64');
  if (payload.format !== ROLLBACK_FORMAT || controlDigest !== expectedDigest ||
      !Number.isSafeInteger(payload.controlSequence) || payload.controlSequence <= 0 ||
      !Number.isSafeInteger(payload.issuedAt) || payload.issuedAt <= 0 ||
      !Number.isSafeInteger(payload.expiresAt) || payload.expiresAt <= now ||
      payload.expiresAt <= payload.issuedAt || payload.issuedAt > now + 5000 ||
      !/^[A-Za-z0-9._:-]{1,120}$/.test(payload.targetPackageId || '') ||
      !/^[a-f0-9]{64}$/.test(payload.targetDigest || '') || !ROLLBACK_REASONS.has(payload.reasonCode) ||
      signatureBytes.length !== 64 || signatureBytes.toString('base64') !== envelope.signature ||
      !crypto.verify(null, Buffer.from(canonical(payload), 'utf8'),
        trustedKeys[envelope.signingKeyId], signatureBytes)) {
    throw new KnowledgeError('ROLLBACK_INVALID', 'Rollback signature, target, sequence, or expiry is invalid.');
  }
  return payload;
}

function createWithdrawalManifest({ controlSequence, issuedAt, expiresAt, signingKeyId,
  privateKey, withdrawnSources }) {
  if (!Number.isSafeInteger(controlSequence) || controlSequence <= 0 ||
      !Number.isSafeInteger(issuedAt) || issuedAt <= 0 ||
      !Number.isSafeInteger(expiresAt) || expiresAt <= issuedAt ||
      !/^[A-Za-z0-9._-]{1,80}$/.test(signingKeyId || '') || !privateKey ||
      !Array.isArray(withdrawnSources) || withdrawnSources.length > 10000) {
    throw new KnowledgeError('INVALID_CONTROL_MANIFEST', 'A signed control manifest requires a sequence, bounded expiry, trusted signing identity, and source list.');
  }
  const seen = new Set();
  const sources = withdrawnSources.map(source => {
    if (!source || !/^[A-Za-z0-9._:-]{1,200}$/.test(source.sourceId || '') ||
        !/^[A-Za-z0-9._:-]{1,100}$/.test(source.version || '') ||
        !WITHDRAWAL_REASONS.has(source.reasonCode)) {
      throw new KnowledgeError('INVALID_CONTROL_MANIFEST', 'A withdrawal entry is malformed.');
    }
    const key = `${source.sourceId}\0${source.version}`;
    if (seen.has(key)) throw new KnowledgeError('INVALID_CONTROL_MANIFEST', 'A source version may appear only once.');
    seen.add(key);
    return { sourceId: source.sourceId, version: source.version, reasonCode: source.reasonCode };
  }).sort((left, right) => `${left.sourceId}:${left.version}`.localeCompare(`${right.sourceId}:${right.version}`));
  const payload = { format: CONTROL_FORMAT, controlSequence, issuedAt, expiresAt,
    issuer: 'Perazim AI knowledge service', withdrawnSources: sources };
  payload.controlDigest = crypto.createHash('sha256').update(canonical(payload), 'utf8').digest('hex');
  const signature = crypto.sign(null, Buffer.from(canonical(payload), 'utf8'), privateKey).toString('base64');
  return { payload, signingKeyId, signature };
}

function verifyWithdrawalManifest(envelope, { trustedKeys, now = Date.now() }) {
  if (!envelope || typeof envelope !== 'object' || !envelope.payload ||
      !/^[A-Za-z0-9._-]{1,80}$/.test(envelope.signingKeyId || '') ||
      !trustedKeys || !trustedKeys[envelope.signingKeyId] ||
      typeof envelope.signature !== 'string' || !/^[A-Za-z0-9+/]+={0,2}$/.test(envelope.signature)) {
    throw new KnowledgeError('CONTROL_UNTRUSTED', 'Control-manifest signing key is not trusted.');
  }
  const payload = envelope.payload;
  const { controlDigest, ...unsigned } = payload;
  const expectedDigest = crypto.createHash('sha256').update(canonical(unsigned), 'utf8').digest('hex');
  const signatureBytes = Buffer.from(envelope.signature, 'base64');
  if (payload.format !== CONTROL_FORMAT || controlDigest !== expectedDigest ||
      !Number.isSafeInteger(payload.controlSequence) || payload.controlSequence <= 0 ||
      !Number.isSafeInteger(payload.issuedAt) || payload.issuedAt <= 0 ||
      !Number.isSafeInteger(payload.expiresAt) || payload.expiresAt <= now ||
      payload.expiresAt <= payload.issuedAt || payload.issuedAt > now + 5000 ||
      payload.issuer !== 'Perazim AI knowledge service' ||
      !Array.isArray(payload.withdrawnSources) || payload.withdrawnSources.length > 10000 ||
      signatureBytes.length !== 64 || signatureBytes.toString('base64') !== envelope.signature ||
      !crypto.verify(null, Buffer.from(canonical(payload), 'utf8'),
        trustedKeys[envelope.signingKeyId], signatureBytes)) {
    throw new KnowledgeError('CONTROL_INVALID', 'Control-manifest signature, expiry, sequence, or format is invalid.');
  }
  const seen = new Set();
  for (const source of payload.withdrawnSources) {
    if (!source || !/^[A-Za-z0-9._:-]{1,200}$/.test(source.sourceId || '') ||
        !/^[A-Za-z0-9._:-]{1,100}$/.test(source.version || '') ||
        !WITHDRAWAL_REASONS.has(source.reasonCode)) {
      throw new KnowledgeError('CONTROL_INVALID', 'A withdrawal entry is malformed.');
    }
    const key = `${source.sourceId}\0${source.version}`;
    if (seen.has(key)) throw new KnowledgeError('CONTROL_INVALID', 'A duplicate withdrawal entry was included.');
    seen.add(key);
  }
  return payload;
}

function compareVersions(left, right) {
  const parse = value => String(value).split('.').map(part => /^\d+$/.test(part) ? Number(part) : -1);
  const a = parse(left), b = parse(right);
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const delta = (a[i] || 0) - (b[i] || 0);
    if (delta) return Math.sign(delta);
  }
  return 0;
}

module.exports = { FORMAT, createOfflinePackage, verifyOfflinePackage,
  ROLLBACK_FORMAT, createRollbackAuthorization, verifyRollbackAuthorization,
  CONTROL_FORMAT, createWithdrawalManifest, verifyWithdrawalManifest };
