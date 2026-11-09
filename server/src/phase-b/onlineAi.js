'use strict';

const crypto = require('node:crypto');
const jwt = require('jsonwebtoken');

const MAX_QUESTION_CHARS = 500;
const ONLINE_PERMISSION_ENV = 'PERAZIM_AI_ONLINE_PERMISSION_CODE';
const CRISIS_PATTERNS = [
  /\b(suicide|suicidal|kill\s+myself|end\s+my\s+life|want\s+to\s+die|hang\s+myself)\b/i,
  /\b(self[\s-]?harm|cutting\s+myself|hurt\s+myself)\b/i,
  /\b(end\s+it\s+all|hopeless.*die|feel\s+like\s+giving\s+up\s+on\s+life)\b/i,
  /\b(abused|physically\s+abused|sexual\s+assault|domestic\s+violence)\b/i
];

class OnlineAiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function validateOnlineAiBody(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw new OnlineAiError(400, 'INVALID_REQUEST', 'A question is required.');
  }
  const keys = Object.keys(body);
  if (keys.some(key => key !== 'question')) {
    throw new OnlineAiError(400, 'INVALID_REQUEST', 'Only a question may be submitted.');
  }
  if (typeof body.question !== 'string' || !body.question.trim() || body.question.length > MAX_QUESTION_CHARS) {
    throw new OnlineAiError(400, 'INVALID_REQUEST', 'A question of 1 to 500 characters is required.');
  }
  return body.question.trim();
}

// Deterministic early rejection for common direct identifiers. This is a
// partial filter only; production enablement also requires a reviewed safety gate.
function containsObviousSensitiveData(question) {
  return /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i.test(question) ||
    /(?:\+?\d[\d\s().-]{7,}\d)/.test(question) ||
    /\b(?:my|our)\s+(?:PIN|password|TOTP|recovery code|medical diagnosis|HIV status|bank account)\b/i.test(question);
}

function isCrisisQuestion(question) { return CRISIS_PATTERNS.some(pattern => pattern.test(String(question || ''))); }

function evaluateOnlineEligibility({ authenticatedUser, membershipStatus, permissionActive, sessionValid = true }) {
  if (!sessionValid || !authenticatedUser || !authenticatedUser.id || authenticatedUser.is_active === false) {
    return { allowed: false, status: 401, code: 'SESSION_INVALID' };
  }
  if (membershipStatus !== 'VERIFIED_MEMBER') {
    return { allowed: false, status: 403, code: 'VERIFIED_MEMBERSHIP_REQUIRED' };
  }
  if (permissionActive !== true) {
    return { allowed: false, status: 403, code: 'ONLINE_AI_PERMISSION_REQUIRED' };
  }
  return { allowed: true, status: 200, code: 'ELIGIBLE' };
}

function workerPseudonym(userId, key) {
  return crypto.createHmac('sha256', key).update(String(userId)).digest('hex');
}

function createWorkerProvider({ workerUrl, sharedSecret, subjectKey, fetchImpl = global.fetch, now = () => Date.now() }) {
  if (!workerUrl || !sharedSecret || sharedSecret.length < 32 || !subjectKey || subjectKey.length < 32 || typeof fetchImpl !== 'function') {
    throw new OnlineAiError(503, 'ONLINE_AI_UNAVAILABLE', 'The online AI service is not configured.');
  }
  return {
    async answer({ userId, question, context = null }) {
      const requestId = crypto.randomUUID();
      const issuedAt = Math.floor(now() / 1000);
      const contextDigest = context ? crypto.createHash('sha256').update(JSON.stringify(context), 'utf8').digest('hex') : null;
      const token = jwt.sign({
        iss: 'perazim-express', aud: 'perazim-ai-worker', iat: issuedAt,
        exp: issuedAt + 60, jti: requestId, sub: workerPseudonym(userId, subjectKey),
        ...(contextDigest ? { ctx: contextDigest } : {})
      }, sharedSecret, { algorithm: 'HS256' });
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 10000);
      try {
        const response = await fetchImpl(workerUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ requestId, question, ...(context ? { context } : {}) }),
          signal: controller.signal
        });
        if (!response.ok) throw new OnlineAiError(response.status === 429 ? 429 : 503,
          response.status === 429 ? 'AI_QUOTA_EXCEEDED' : 'ONLINE_AI_UNAVAILABLE', 'The online AI service is unavailable.');
        const result = await response.json();
        if (!result || (context ? (!result.response || typeof result.response !== 'object') : (typeof result.answer !== 'string' || !result.answer.trim()))) {
          throw new OnlineAiError(503, 'ONLINE_AI_UNAVAILABLE', 'The online AI service returned no answer.');
        }
        return { ...(context ? { response: result.response } : { answer: result.answer.trim() }), remaining: Number.isInteger(result.remaining) ? result.remaining : null };
      } catch (error) {
        if (error instanceof OnlineAiError) throw error;
        throw new OnlineAiError(503, 'ONLINE_AI_UNAVAILABLE', 'The online AI service is unavailable.');
      } finally {
        clearTimeout(timeout);
      }
    }
  };
}

module.exports = {
  MAX_QUESTION_CHARS, ONLINE_PERMISSION_ENV, OnlineAiError,
  validateOnlineAiBody, containsObviousSensitiveData, isCrisisQuestion, evaluateOnlineEligibility,
  workerPseudonym, createWorkerProvider
};
