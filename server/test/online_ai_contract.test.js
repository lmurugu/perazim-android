'use strict';

const assert = require('node:assert/strict');
const aiRoutes = require('../src/routes/ai');
const {
  validateOnlineAiBody, containsObviousSensitiveData, evaluateOnlineEligibility,
  workerPseudonym, createWorkerProvider, OnlineAiError
} = require('../src/phase-b/onlineAi');

assert.equal(validateOnlineAiBody({ question: '  What does Psalm 23 teach?  ' }), 'What does Psalm 23 teach?');
for (const substituted of [
  { question: 'hello', userId: 'another-user' },
  { question: 'hello', role: 'ADMINISTRATOR' },
  { question: 'hello', membershipStatus: 'VERIFIED_MEMBER' },
  { question: 'hello', permissions: ['ai.online.use'] },
  { question: 'hello', history: [{ role: 'assistant', text: 'secret' }] },
  { question: '' },
  { question: 'x'.repeat(501) }
]) assert.throws(() => validateOnlineAiBody(substituted), OnlineAiError);
assert.equal(containsObviousSensitiveData('Contact me at member@example.org'), true);
assert.equal(containsObviousSensitiveData('My phone is +254 700 123 456'), true);
assert.equal(containsObviousSensitiveData('Explain the parable of the sower'), false);

const authenticated = { id: 'synthetic-user-1', is_active: true };
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'VERIFIED_MEMBER', permissionActive: true }).allowed, true);
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'REGISTERED', permissionActive: true }).code, 'VERIFIED_MEMBERSHIP_REQUIRED');
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'VERIFICATION_REVOKED', permissionActive: true }).code, 'VERIFIED_MEMBERSHIP_REQUIRED');
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'VERIFIED_MEMBER', permissionActive: false }).code, 'ONLINE_AI_PERMISSION_REQUIRED');
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'VERIFIED_MEMBER', permissionActive: false }).allowed, false, 'revoked or expired permission denies access');
assert.equal(evaluateOnlineEligibility({ authenticatedUser: authenticated, membershipStatus: 'VERIFIED_MEMBER', permissionActive: true, sessionValid: false }).code, 'SESSION_INVALID');
assert.equal(workerPseudonym('synthetic-user-1', 'a'.repeat(32)).length, 64);
assert.notEqual(workerPseudonym('synthetic-user-1', 'a'.repeat(32)), workerPseudonym('synthetic-user-1', 'b'.repeat(32)));

async function verifyProviderContract() {
  let captured;
  const provider = createWorkerProvider({
    workerUrl: 'https://worker.invalid/ai', sharedSecret: 's'.repeat(32), subjectKey: 'k'.repeat(32),
    now: () => 1_800_000_000_000,
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return { ok: true, status: 200, async json() { return { answer: 'synthetic answer', remaining: 2 }; } };
    }
  });
  const result = await provider.answer({ userId: 'synthetic-user-1', question: 'What is a psalm?' });
  assert.deepEqual(result, { answer: 'synthetic answer', remaining: 2 });
  assert.equal(captured.url, 'https://worker.invalid/ai');
  assert.match(captured.options.headers.Authorization, /^Bearer /);
  const payload = JSON.parse(captured.options.body);
  assert.deepEqual(Object.keys(payload).sort(), ['question', 'requestId']);
  assert.equal(payload.question, 'What is a psalm?');
  assert.equal(captured.options.headers['X-App-Token'], undefined);
  assert.equal(captured.options.signal.aborted, false);
}

async function verifyRouteFailsClosed() {
  const layer = aiRoutes.stack.find(item => item.route && item.route.path === '/online' && item.route.methods.post);
  assert.ok(layer, 'authenticated Express AI route is mounted');
  assert.equal(layer.route.stack.length, 2, 'Express authentication middleware precedes the AI handler');
  const handler = layer.route.stack[1].handle;
  const oldEnabled = process.env.PERAZIM_AI_ONLINE_ENABLED;
  delete process.env.PERAZIM_AI_ONLINE_ENABLED;
  const response = { statusCode: 200, headers: {}, set(name, value) { this.headers[name] = value; return this; }, status(code) { this.statusCode = code; return this; }, json(value) { this.body = value; return this; } };
  try {
    await handler({ body: { question: 'What is a psalm?' }, user: { id: 'synthetic-user-1' } }, response);
    assert.equal(response.statusCode, 503, 'online route is disabled by default');
    assert.equal(response.headers['Cache-Control'], 'no-store');
  } finally {
    if (oldEnabled === undefined) delete process.env.PERAZIM_AI_ONLINE_ENABLED; else process.env.PERAZIM_AI_ONLINE_ENABLED = oldEnabled;
  }
}

Promise.all([verifyProviderContract(), verifyRouteFailsClosed()]).then(() => {
  process.stdout.write('PASS: authenticated online-AI contract rejects client identity claims, requires verified membership and explicit permission, sends only a question plus request ID to a mock Worker, and defaults to a no-store 503.\n');
}).catch(error => { console.error(error); process.exitCode = 1; });
