'use strict';

const express = require('express');
const { query } = require('../db');
const { requireAuth } = require('../middleware/auth');
const {
  OnlineAiError, ONLINE_PERMISSION_ENV, validateOnlineAiBody,
  containsObviousSensitiveData, evaluateOnlineEligibility, createWorkerProvider
} = require('../phase-b/onlineAi');
const { retrieveInstitutionalContext } = require('../phase-b/governedKnowledge');

const router = express.Router();

router.post('/online', requireAuth, async (req, res) => {
  res.set('Cache-Control', 'no-store');
  try {
    const question = validateOnlineAiBody(req.body);
    if (containsObviousSensitiveData(question)) {
      throw new OnlineAiError(422, 'SENSITIVE_CONTENT_UNSUPPORTED', 'Please remove identifying or sensitive details. This service cannot safely handle them.');
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

    const provider = createWorkerProvider({
      workerUrl: process.env.PERAZIM_AI_WORKER_URL,
      sharedSecret: process.env.PERAZIM_AI_WORKER_SHARED_SECRET,
      subjectKey: process.env.PERAZIM_AI_SUBJECT_PSEUDONYM_KEY
    });
    const result = await provider.answer({ userId: req.user.id, question, context });
    return res.json({ answer: result.answer, remaining: result.remaining, sourceReferences, sourceSufficiency: 'SUFFICIENT' });
  } catch (error) {
    if (error instanceof OnlineAiError) return res.status(error.status).json({ error: error.message, code: error.code });
    // Never log raw prompts, tokens, provider details, or database values.
    return res.status(503).json({ error: 'Online AI is temporarily unavailable.', code: 'ONLINE_AI_UNAVAILABLE' });
  }
});

module.exports = router;
