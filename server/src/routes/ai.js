'use strict';

const express = require('express');
const crypto = require('node:crypto');
const { query, withTransaction, isPostgresActive } = require('../db');
const { reserveOnlineAiBudget } = require('../phase-b/onlineAiBudget');
const { createOfflinePackage, createWithdrawalManifest, createRollbackAuthorization,
  verifyRollbackAuthorization } = require('../phase-b/offlineKnowledgePackage');
const { requireAdminAuthority, writeAudit, PhaseBError } = require('../phase-b/security');
const { requireAuth } = require('../middleware/auth');
const {
  OnlineAiError, ONLINE_PERMISSION_ENV, validateOnlineAiBody,
  containsObviousSensitiveData, containsPrivatePastoralContent, isCrisisQuestion,
  isClearlyOutOfScopeQuestion, evaluateOnlineEligibility, createWorkerProvider
} = require('../phase-b/onlineAi');
const { retrieveInstitutionalContext } = require('../phase-b/governedKnowledge');
const { retrieveScriptureContext } = require('../phase-b/scriptureCorpus');
const { validateModelResponse } = require('../phase-b/responseValidation');

const router = express.Router();

router.use((req, res, next) => {
  res.set('Cache-Control', 'no-store');
  next();
});
router.use(express.json({ limit: '4kb', inflate: false }));
router.use((error, req, res, next) => {
  const status = [400, 413, 415].includes(error.status) ? error.status : 503;
  return res.status(status).json({ error: 'The online AI request could not be read.', code: 'INVALID_REQUEST' });
});

router.get('/offline-package/current', requireAuth, async (_req, res) => {
  res.set('Cache-Control', 'private, no-store');
  try {
    if (process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED !== 'true' ||
        !process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID ||
        !process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY ||
        !process.env.PERAZIM_AI_PACKAGE_MIN_APP_VERSION ||
        !/^\d+$/.test(process.env.PERAZIM_AI_PACKAGE_TTL_MS || '')) {
      throw new OnlineAiError(503, 'OFFLINE_PACKAGE_UNAVAILABLE', 'Offline knowledge updates are unavailable.');
    }
    const now = Date.now();
    const packageTtlMs = Number(process.env.PERAZIM_AI_PACKAGE_TTL_MS);
    const expiresAt = now + packageTtlMs;
    if (!Number.isSafeInteger(packageTtlMs) || packageTtlMs <= 0 || !Number.isSafeInteger(expiresAt)) {
      throw new OnlineAiError(503, 'OFFLINE_PACKAGE_UNAVAILABLE', 'Offline knowledge updates are unavailable.');
    }
    const result = await query(
      `SELECT s.source_id, s.version, s.title, s.authority_class, s.approval_status,
              s.publication_status, s.confidentiality, s.permitted_audiences,
              s.permitted_uses, s.approval_authority, s.approval_reference,
              s.approved_at, c.location, c.content_hash, c.content AS text,
              s.language, s.translation_id AS license
         FROM ai_knowledge_sources s
         JOIN ai_knowledge_chunks c ON c.source_id=s.source_id AND c.source_version=s.version
        WHERE s.approval_status='APPROVED' AND s.publication_status='PUBLISHED'
          AND s.confidentiality='PUBLIC'
          AND (s.effective_at IS NULL OR s.effective_at <= $1)
          AND s.permitted_audiences @> '["PUBLIC"]'::jsonb
          AND s.permitted_uses @> '["OFFLINE"]'::jsonb
        ORDER BY s.source_id, s.version, c.location, c.chunk_id`, [Date.now()]
    );
    return res.json(createOfflinePackage({
      packageId: 'perazim-public-knowledge', version: String(now), publishedAt: now, expiresAt,
      minimumAppVersion: process.env.PERAZIM_AI_PACKAGE_MIN_APP_VERSION,
      signingKeyId: process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID,
      privateKey: process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY.replace(/\\n/g, '\n'),
      sources: result.rows, allowEmptySnapshot: true
    }));
  } catch (error) {
    if (error instanceof OnlineAiError) return res.status(error.status).json({ error: error.message, code: error.code });
    return res.status(503).json({ error: 'Offline knowledge updates are unavailable.', code: 'OFFLINE_PACKAGE_UNAVAILABLE' });
  }
});

router.get('/offline-package/control/current', requireAuth, async (_req, res) => {
  res.set('Cache-Control', 'private, no-store');
  try {
    if (process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED !== 'true' ||
        !process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID ||
        !process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY ||
        !/^\d+$/.test(process.env.PERAZIM_AI_OFFLINE_CONTROL_TTL_MS || '')) {
      throw new OnlineAiError(503, 'OFFLINE_CONTROL_UNAVAILABLE', 'Offline knowledge status is unavailable.');
    }
    const now = Date.now();
    const ttlMs = Number(process.env.PERAZIM_AI_OFFLINE_CONTROL_TTL_MS);
    const expiresAt = now + ttlMs;
    if (!Number.isSafeInteger(ttlMs) || ttlMs <= 0 || !Number.isSafeInteger(expiresAt)) {
      throw new OnlineAiError(503, 'OFFLINE_CONTROL_UNAVAILABLE', 'Offline knowledge status is unavailable.');
    }
    const result = await query(
      `WITH control AS (
         SELECT nextval('ai_offline_control_sequence')::text AS sequence
       ), withdrawn AS (
         SELECT COALESCE(jsonb_agg(jsonb_build_object(
             'sourceId', s.source_id,
             'version', s.version,
             'reasonCode', CASE WHEN s.approval_status='SUPERSEDED' THEN 'REPLACED' ELSE s.withdrawal_reason END
           ) ORDER BY s.source_id, s.version), '[]'::jsonb) AS sources
           FROM ai_knowledge_sources s
          WHERE s.approval_status IN ('WITHDRAWN','SUPERSEDED')
            AND s.confidentiality='PUBLIC'
            AND s.permitted_audiences @> '["PUBLIC"]'::jsonb
            AND s.permitted_uses @> '["OFFLINE"]'::jsonb
       )
       SELECT control.sequence, withdrawn.sources FROM control CROSS JOIN withdrawn`
    );
    const sequence = Number(result.rows[0]?.sequence);
    if (!Number.isSafeInteger(sequence) || sequence <= 0) {
      throw new OnlineAiError(503, 'OFFLINE_CONTROL_UNAVAILABLE', 'Offline knowledge status is unavailable.');
    }
    return res.json(createWithdrawalManifest({
      controlSequence: sequence, issuedAt: now, expiresAt,
      signingKeyId: process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID,
      privateKey: process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY.replace(/\\n/g, '\n'),
      withdrawnSources: result.rows[0].sources
    }));
  } catch (error) {
    if (error instanceof OnlineAiError) return res.status(error.status).json({ error: error.message, code: error.code });
    return res.status(503).json({ error: 'Offline knowledge status is unavailable.', code: 'OFFLINE_CONTROL_UNAVAILABLE' });
  }
});

router.post('/offline-package/rollback/authorize', requireAuth, async (req, res) => {
  res.set('Cache-Control', 'private, no-store');
  try {
    const body = req.body || {};
    const requestId = body.requestId;
    const targetPackageId = body.targetPackageId;
    const targetDigest = body.targetDigest;
    const reasonCode = body.reasonCode;
    if (typeof requestId !== 'string' ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(requestId) ||
        !/^[A-Za-z0-9._:-]{1,120}$/.test(targetPackageId || '') ||
        !/^[a-f0-9]{64}$/.test(targetDigest || '') ||
        !['PACKAGE_ROLLBACK', 'SOURCE_CORRECTION', 'SECURITY_REMEDIATION'].includes(reasonCode)) {
      throw new OnlineAiError(400, 'INVALID_ROLLBACK_REQUEST', 'An exact rollback target, reason, and request identifier are required.');
    }
    if (process.env.PERAZIM_AI_OFFLINE_PACKAGE_EXPORT_ENABLED !== 'true' ||
        process.env.PERAZIM_AI_OFFLINE_ROLLBACK_AUTHORIZATION_ENABLED !== 'true' ||
        !process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID || !process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY ||
        !/^\d+$/.test(process.env.PERAZIM_AI_ROLLBACK_TTL_MS || '') || !isPostgresActive()) {
      throw new OnlineAiError(503, 'OFFLINE_ROLLBACK_UNAVAILABLE', 'Offline rollback authorization is unavailable.');
    }
    const now = Date.now();
    const ttlMs = Number(process.env.PERAZIM_AI_ROLLBACK_TTL_MS);
    const expiresAt = now + ttlMs;
    if (!Number.isSafeInteger(ttlMs) || ttlMs <= 0 || ttlMs > 7 * 24 * 60 * 60 * 1000 ||
        !Number.isSafeInteger(expiresAt)) {
      throw new OnlineAiError(503, 'OFFLINE_ROLLBACK_UNAVAILABLE', 'Offline rollback authorization is unavailable.');
    }
    const privilegedToken = req.get('X-Privileged-Session') || '';
    if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required.');
    const envelope = await withTransaction(async client => {
      await requireAdminAuthority(client, {
        userId: req.user.id, authToken: req.token, privilegedToken,
        capability: 'knowledge.package.rollback.authorize', scope: 'global', lock: true
      });
      const replay = await client.query(
        'SELECT 1 FROM ai_offline_rollback_authorizations WHERE actor_id=$1 AND request_id=$2',
        [req.user.id, requestId]
      );
      if (replay.rows.length) throw new PhaseBError(409, 'REQUEST_ALREADY_PROCESSED', 'This rollback request was already processed.');
      const sequenceResult = await client.query("SELECT nextval('ai_offline_control_sequence')::text AS sequence");
      const controlSequence = Number(sequenceResult.rows[0]?.sequence);
      if (!Number.isSafeInteger(controlSequence) || controlSequence <= 0) {
        throw new OnlineAiError(503, 'OFFLINE_ROLLBACK_UNAVAILABLE', 'Offline rollback authorization is unavailable.');
      }
      const signed = createRollbackAuthorization({
        controlSequence, issuedAt: now, expiresAt, targetPackageId, targetDigest, reasonCode,
        signingKeyId: process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID,
        privateKey: process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY.replace(/\\n/g, '\n')
      });
      verifyRollbackAuthorization(signed, {
        trustedKeys: { [process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID]:
          crypto.createPublicKey(process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY.replace(/\\n/g, '\n')) },
        now
      });
      await client.query(
        `INSERT INTO ai_offline_rollback_authorizations
           (authorization_id,actor_id,target_package_id,target_digest,control_sequence,issued_at,expires_at,
            reason_code,request_id,authorization_envelope)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10::jsonb)`,
        [crypto.randomUUID(), req.user.id, targetPackageId, targetDigest,
          controlSequence, now, expiresAt, reasonCode, requestId, JSON.stringify(signed)]
      );
      await writeAudit(client, { actorId: req.user.id, action: 'AI_PACKAGE_ROLLBACK_AUTHORIZE',
        resourceType: 'ai_offline_package', resourceId: `${targetPackageId}:${targetDigest}`,
        reason: reasonCode, requestId, createdAt: now });
      return signed;
    });
    return res.status(201).json(envelope);
  } catch (error) {
    if (error instanceof OnlineAiError || error instanceof PhaseBError) {
      return res.status(error.status).json({ error: error.message, code: error.code });
    }
    if (error && error.code === '23505') {
      return res.status(409).json({ error: 'This rollback request was already processed.', code: 'REQUEST_ALREADY_PROCESSED' });
    }
    return res.status(503).json({ error: 'Offline rollback authorization is unavailable.', code: 'OFFLINE_ROLLBACK_UNAVAILABLE' });
  }
});

router.get('/offline-package/rollback/current', requireAuth, async (req, res) => {
  res.set('Cache-Control', 'private, no-store');
  try {
    const targetPackageId = req.query.targetPackageId;
    const targetDigest = req.query.targetDigest;
    if (!/^[A-Za-z0-9._:-]{1,120}$/.test(targetPackageId || '') || !/^[a-f0-9]{64}$/.test(targetDigest || '')) {
      throw new OnlineAiError(400, 'INVALID_ROLLBACK_TARGET', 'An exact rollback target is required.');
    }
    if (process.env.PERAZIM_AI_OFFLINE_ROLLBACK_AUTHORIZATION_ENABLED !== 'true' ||
        !process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID || !process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY ||
        !isPostgresActive()) {
      throw new OnlineAiError(503, 'OFFLINE_ROLLBACK_UNAVAILABLE', 'Offline rollback authorization is unavailable.');
    }
    const result = await query(
      `SELECT authorization_envelope,control_sequence,issued_at,expires_at,reason_code
         FROM ai_offline_rollback_authorizations
        WHERE target_package_id=$1 AND target_digest=$2 AND expires_at>$3
        ORDER BY control_sequence DESC LIMIT 1`, [targetPackageId, targetDigest, Date.now()]
    );
    if (!result.rows.length) throw new OnlineAiError(404, 'ROLLBACK_NOT_AUTHORIZED', 'No current authorization exists for this package.');
    const row = result.rows[0];
    const envelope = row.authorization_envelope;
    const trustedKey = crypto.createPublicKey(process.env.PERAZIM_AI_PACKAGE_PRIVATE_KEY.replace(/\\n/g, '\n'));
    const verified = verifyRollbackAuthorization(envelope, {
      trustedKeys: { [process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID]: trustedKey }, now: Date.now()
    });
    if (envelope.signingKeyId !== process.env.PERAZIM_AI_PACKAGE_SIGNING_KEY_ID ||
        verified.targetPackageId !== targetPackageId || verified.targetDigest !== targetDigest ||
        verified.controlSequence !== Number(row.control_sequence) || verified.issuedAt !== Number(row.issued_at) ||
        verified.expiresAt !== Number(row.expires_at) || verified.reasonCode !== row.reason_code) {
      throw new OnlineAiError(503, 'OFFLINE_ROLLBACK_UNAVAILABLE', 'Offline rollback authorization is unavailable.');
    }
    return res.json(envelope);
  } catch (error) {
    if (error instanceof OnlineAiError) return res.status(error.status).json({ error: error.message, code: error.code });
    return res.status(503).json({ error: 'Offline rollback authorization is unavailable.', code: 'OFFLINE_ROLLBACK_UNAVAILABLE' });
  }
});

router.post('/online', requireAuth, async (req, res) => {
  res.set('Cache-Control', 'no-store');
  try {
    const question = validateOnlineAiBody(req.body);
    if (isCrisisQuestion(question)) {
      return res.json({
        answer: 'You matter, and you do not have to face this alone. If you or someone else is in immediate danger, contact local emergency services or go to the nearest hospital now. If you can, reach a trusted person who can stay with you and help you get support.',
        remaining: null, sourceReferences: [], sourceSufficiency: 'NOT_APPLICABLE', escalationRequired: true
      });
    }
    if (containsObviousSensitiveData(question) || containsPrivatePastoralContent(question)) {
      throw new OnlineAiError(422, 'SENSITIVE_CONTENT_UNSUPPORTED', 'Please remove identifying or sensitive details. This service cannot safely handle them.');
    }
    if (isClearlyOutOfScopeQuestion(question)) {
      throw new OnlineAiError(422, 'AI_SCOPE_RESTRICTED',
        'Perazim AI helps with Christian faith, approved church information, and app use; it cannot provide investment, medical, or legal advice or answer unrelated general-interest questions.');
    }
    // These independent server-side gates remain off until the entitlement,
    // privacy/safety, quota, and deployed Worker contracts are operationally verified.
    if (process.env.PERAZIM_AI_ONLINE_ENABLED !== 'true' ||
        process.env.PERAZIM_AI_ONLINE_PRIVACY_REVIEWED !== 'true' ||
        process.env.PERAZIM_AI_ONLINE_QUOTA_ENFORCED !== 'true' ||
        process.env.PERAZIM_AI_WORKER_CONTRACT !== 'signed-v1') {
      throw new OnlineAiError(503, 'ONLINE_AI_UNAVAILABLE', 'Online AI is unavailable while its secure service is being verified.');
    }
    const permissionCode = process.env[ONLINE_PERMISSION_ENV];
    if (!permissionCode || !/^[a-z][a-z0-9._-]{2,99}$/.test(permissionCode)) {
      throw new OnlineAiError(503, 'AI_PERMISSION_CONTRACT_UNCONFIGURED', 'Online AI access is not configured.');
    }

    const membership = await query('SELECT status FROM membership_verifications WHERE user_id=$1', [req.user.id]);
    const membershipStatus = membership.rows[0]?.status || 'REGISTERED';
    const permission = await query(
      `SELECT 1 FROM user_permissions up JOIN permissions p ON p.code=up.permission_code
        WHERE up.user_id=$1 AND up.permission_code=$2 AND p.active=TRUE
          AND up.revoked_at IS NULL AND up.effective_at <= $3
          AND (up.expires_at IS NULL OR up.expires_at > $3)
          AND (up.scope='global' OR up.scope=$4) LIMIT 1`,
      [req.user.id, permissionCode, Date.now(), 'global']
    );
    const eligibility = evaluateOnlineEligibility({
      authenticatedUser: req.user,
      membershipStatus,
      permissionActive: permission.rows.length > 0,
      sessionValid: true
    });
    if (!eligibility.allowed) throw new OnlineAiError(eligibility.status, eligibility.code, 'Online AI access is not available for this account.');

    const context = await retrieveInstitutionalContext(query, question);
    if (!context.sufficient) {
      return res.json({
        answer: 'I do not have an approved Perazim source available to verify an institutional answer to this question. I cannot present an unverified answer as the church’s position.',
        remaining: null,
        sourceReferences: [],
        sourceSufficiency: 'INSUFFICIENT'
      });
    }
    const sourceReferences = context.passages.map(p => ({
      sourceId: p.sourceId, title: p.title, version: p.version,
      authorityClass: p.authorityClass, approvalStatus: p.approvalStatus,
      location: p.location, contentHash: p.contentHash
    }));
    if (context.conflicts.length) {
      return res.json({
        answer: 'I found approved Perazim sources that are explicitly marked as conflicting. I cannot resolve that conflict or state an institutional position; this question needs review by the appropriate church authority.',
        remaining: null, sourceReferences, sourceSufficiency: 'CONFLICTED',
        conflicts: context.conflicts, escalationRequired: true
      });
    }

    const scriptures = await retrieveScriptureContext(query, question, process.env.PERAZIM_AI_SCRIPTURE_TRANSLATION);
    if (!scriptures.sufficient) {
      return res.json({
        answer: 'I found approved Perazim material, but I could not retrieve a matching exact Scripture passage from an authorized corpus. I cannot supply or reconstruct a Bible quotation from memory.',
        remaining: null, sourceReferences, sourceSufficiency: 'SCRIPTURE_UNAVAILABLE', escalationRequired: true
      });
    }
    const signedContext = { ...context, scriptures: scriptures.passages };

    const provider = createWorkerProvider({
      workerUrl: process.env.PERAZIM_AI_WORKER_URL,
      sharedSecret: process.env.PERAZIM_AI_WORKER_SHARED_SECRET,
      subjectKey: process.env.PERAZIM_AI_SUBJECT_PSEUDONYM_KEY
    });
    if (!isPostgresActive()) {
      throw new OnlineAiError(503, 'AI_BUDGET_UNAVAILABLE', 'The online service budget is unavailable.');
    }
    const remaining = await reserveOnlineAiBudget(withTransaction, {
      userId: req.user.id,
      subjectKey: process.env.PERAZIM_AI_SUBJECT_PSEUDONYM_KEY,
      globalLimit: process.env.PERAZIM_AI_GLOBAL_DAILY_LIMIT
    });
    const result = await provider.answer({ userId: req.user.id, question, context: signedContext });
    const validated = validateModelResponse(result.response, signedContext);
    if (!validated.valid) {
      return res.json({ answer: validated.answer, remaining,
        sourceReferences: [], scriptureReferences: [], sourceSufficiency: 'INVALID_MODEL_RESPONSE',
        escalationRequired: validated.escalationRequired, limitationCode: validated.code });
    }
    return res.json({ ...validated.response, remaining, sourceSufficiency: 'SUFFICIENT' });
  } catch (error) {
    if (error instanceof OnlineAiError) return res.status(error.status).json({ error: error.message, code: error.code });
    // Never log raw prompts, tokens, provider details, or database values.
    return res.status(503).json({ error: 'Online AI is temporarily unavailable.', code: 'ONLINE_AI_UNAVAILABLE' });
  }
});

router.use((_req, res) => res.status(404).json({ error: 'AI route not found.' }));

module.exports = router;
