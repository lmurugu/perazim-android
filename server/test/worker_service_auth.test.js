'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const crypto = require('node:crypto');
const jwt = require('jsonwebtoken');

const sourcePath = require('node:path').resolve(__dirname, '../../relay/worker.js');
const source = fs.readFileSync(sourcePath, 'utf8').replace('export default {', 'module.exports.default = {');
const moduleObject = { exports: {} };
let upstreamCalls = 0;
let upstreamBody = null;
const sandbox = {
  module: moduleObject, exports: moduleObject.exports,
  URL, Request, Response, Headers, AbortController, TextEncoder, TextDecoder, Uint8Array,
  crypto: crypto.webcrypto, atob, Date, Math, JSON, Number, String, Array, Object, RegExp,
  setTimeout, clearTimeout,
  fetch: async (_url, options) => {
    upstreamCalls += 1;
    upstreamBody = JSON.parse(options.body);
    return new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: 'Synthetic grounded answer' }] } }] }), { status: 200 });
  },
  console: { error() {}, warn() {}, log() {} }
};
vm.runInNewContext(source, sandbox, { filename: sourcePath });
const worker = moduleObject.exports.default;
const secret = crypto.randomBytes(32).toString('base64url');
const geminiTestKey = crypto.randomBytes(24).toString('hex');
const envBase = { AI_ENABLED: 'true', GEMINI_API_KEY: geminiTestKey, PERAZIM_AI_WORKER_SHARED_SECRET: secret };

function grantToken(overrides = {}) {
  const now = Math.floor(Date.now() / 1000);
  return jwt.sign({
    iss: 'perazim-express', aud: 'perazim-ai-worker', iat: now,
    exp: now + 60, jti: crypto.randomUUID(), sub: 'a'.repeat(64), ...overrides
  }, secret, { algorithm: 'HS256' });
}
function request(token, body, headers = {}) {
  return new Request('https://worker.test/chat', {
    method: 'POST', headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: JSON.stringify(body)
  });
}
async function status(response) { return response.status; }

async function run() {
  const directLegacy = await worker.fetch(request(null, { question: 'hello', deviceId: 'client' }, { 'X-App-Token': 'synthetic-old-token' }), { ...envBase, APP_TOKEN: 'synthetic-old-token' });
  assert.equal(await status(directLegacy), 401, 'legacy Android shared token is rejected');
  assert.equal(upstreamCalls, 0);

  const expiredToken = grantToken({ exp: Math.floor(Date.now() / 1000) - 1 });
  const expired = await worker.fetch(request(expiredToken, { question: 'hello', requestId: jwt.decode(expiredToken).jti }), { ...envBase, PERAZIM_KV: { async get() { return null; }, async put() {} } });
  assert.equal(await status(expired), 401, 'expired backend grant is rejected');
  const wrongAudience = grantToken({ aud: 'another-service' });
  const wrongTarget = await worker.fetch(request(wrongAudience, { question: 'hello', requestId: jwt.decode(wrongAudience).jti }), { ...envBase, PERAZIM_KV: { async get() { return null; }, async put() {} } });
  assert.equal(await status(wrongTarget), 401, 'grant for another service is rejected');

  const token = grantToken();
  const substituted = await worker.fetch(request(token, { question: 'hello', requestId: jwt.decode(token).jti, userId: 'other-user' }), { ...envBase, PERAZIM_KV: { async get() { return null; }, async put() {} } });
  assert.equal(await status(substituted), 400, 'client identity/context fields are rejected');
  assert.equal(upstreamCalls, 0);

  const noKv = await worker.fetch(request(token, { question: 'hello', requestId: jwt.decode(token).jti }), envBase);
  assert.equal(await status(noKv), 503, 'quota dependency absence fails closed before Gemini');
  assert.equal(upstreamCalls, 0);

  const kv = { async get() { return null; }, async put() {} };
  const response = await worker.fetch(request(token, { question: 'What is a psalm?', requestId: jwt.decode(token).jti }), { ...envBase, PERAZIM_KV: kv });
  assert.equal(await status(response), 200);
  const result = await response.json();
  assert.equal(result.answer, 'Synthetic grounded answer');
  assert.equal(upstreamCalls, 1);
  assert.equal(JSON.stringify(upstreamBody).includes('deviceId'), false);
  assert.equal(JSON.stringify(upstreamBody).includes('Member Name'), false);
  assert.equal(JSON.stringify(upstreamBody).includes('streak'), false);
  process.stdout.write('PASS: Worker rejects the old client token and identity substitution, fails closed without KV, and forwards only a minimally scoped question under a signed synthetic service grant.\n');
}

run().catch(error => { console.error(error); process.exitCode = 1; });
