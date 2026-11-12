'use strict';

const crypto = require('node:crypto');
const jwt = require('jsonwebtoken');

const MAX_QUESTION_CHARS = 500;
const ONLINE_PERMISSION_ENV = 'PERAZIM_AI_ONLINE_PERMISSION_CODE';
const CRISIS_PATTERNS = [
  /\b(suicide|suicidal|kill\s+myself|end\s+my\s+life|want\s+to\s+die|hang\s+myself)\b/i,
  /\b(self[\s-]?harm|cutting\s+myself|hurt\s+myself)\b/i,
  /\b(end\s+it\s+all|hopeless.*die|feel\s+like\s+giving\s+up\s+on\s+life)\b/i,
  /\b(physically\s+abused|sexual\s+assault|domestic\s+violence)\b/i,
  /\b(better\s+off\s+dead|(?:can['’]t|cannot)\s+go\s+on|not\s+worth\s+living|harm\s+myself|unalive\s+myself)\b/i,
  /\b(?:(?:going\s+to|plan\s+to|want\s+to)\s+(?:kill|shoot|stab)\s+(?:someone|him|her|them)|(?:immediate|imminent)\s+danger)\b/i,
  /\b(?:(?:my|our)\s+(?:husband|wife|spouse|partner|boyfriend|girlfriend|parent|father|mother|caregiver)\s+(?:hits?|beats?|beating|struck|choked|attacked|threatened)\s+(?:me|us|my\s+child|our\s+child)|(?:hit|hits|hitting|beat|beats|beating|struck|choked|choking|attacked|threatened)\s+(?:me|us|my\s+child|our\s+child))\b/i,
  /\b(?:afraid|scared|fear)\s+(?:that\s+)?(?:my\s+)?(?:partner|husband|wife|spouse|parent|father|mother|caregiver)\s+(?:will|might|may|is\s+going\s+to)\s+(?:hurt|hit|beat|attack|kill)\s+(?:me|my\s+child|our\s+child)\b/i
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

function containsPrivatePastoralContent(question) {
  const value = String(question || '');
  const directDisclosure = /\b(?:my|our)\s+(?:pastor(?:al)?|counsell?or|therapist|prayer request|counsell?ing session|confession)\b/i.test(value) ||
    /\b(?:pray for me|keep this confidential|please keep this secret|pastoral intervention)\b/i.test(value);
  const personalContext = /\b(?:me|my|mine|we|us|our|ours|my friend|someone close to me|my family|my child|our child)\b/i.test(value) ||
    /\b(?:i am|i['’]m|i have been|i was|i feel|i felt|i experienced|i went through|i['’]ve been)\b/i.test(value);
  const sensitiveTopic = /\b(?:abuse|assault|rape|violence|hit(?:s|ting)?|beat(?:s|ing)?|chok(?:e|ed|ing)|attacked|threatened|threatening|marital|marriage conflict|divorce|separation|affair|family conflict|trauma|grief|bereavement|mourning|depress(?:ion|ed)?|anx(?:iety|ious)|panic attack|self[\s-]?harm|suicid(?:e|al)|addict(?:ion|ed)|overdose|pregnan(?:t|cy)|miscarriage|cancer|HIV|diagnos(?:is|ed)|confession|confidential|secret)\b/i.test(value);
  return directDisclosure || (personalContext && sensitiveTopic);
}

function isCrisisQuestion(question) { return CRISIS_PATTERNS.some(pattern => pattern.test(String(question || ''))); }

// Hard stop for clearly unrelated professional/general-interest requests.
// This complements, rather than replaces, the Worker's behavioural instruction.
const CLEARLY_OUT_OF_SCOPE_PATTERNS = [
  /\b(?:which|what)\s+(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\b/i,
  /\b(?:buy|sell|trade|invest in)\b.{0,70}\b(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\b/i,
  /\b(?:should i|recommend|help me choose|help me pick)\b.{0,70}\b(?:investment portfolio|retirement portfolio|personal budget|payday loan|mortgage|credit card|tax return|insurance policy)\b/i,
  /\b(?:what|which|recommend|prescribe)\b.{0,50}\b(?:medicine|medication|drug|dosage|dose|treatment)\b|\b(?:diagnose me|diagnose my|medical advice|prescribe medication)\b/i,
  /\b(?:give me legal advice|should i sue|how (?:do|can) i sue|file (?:a )?lawsuit|draft (?:a )?(?:legal )?contract|represent me in court)\b/i,
  /\b(?:who won|latest score|current score|score of)\b.{0,60}\b(?:football|match|game|election|sports?)\b|\b(?:write|draft) (?:me )?a campaign speech\b/i
];
function isClearlyOutOfScopeQuestion(question) {
  const value = String(question || '').replace(/[’]/g, "'").replace(/\s+/g, ' ').trim();
  return CLEARLY_OUT_OF_SCOPE_PATTERNS.some(pattern => pattern.test(value));
}

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
  let url;
  try { url = new URL(workerUrl); } catch (_) {}
  if (!url || url.protocol !== 'https:' || url.username || url.password || url.hash || url.search) {
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
          signal: controller.signal,
          redirect: 'error'
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
  validateOnlineAiBody, containsObviousSensitiveData, containsPrivatePastoralContent, isCrisisQuestion,
  isClearlyOutOfScopeQuestion, evaluateOnlineEligibility,
  workerPseudonym, createWorkerProvider
};
