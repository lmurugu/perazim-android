'use strict';

const assert = require('assert');
const { memoryStore } = require('../src/db');
const authRoutes = require('../src/routes/auth');
const privacyRoutes = require('../src/routes/privacy');
const { hashContent } = require('../src/phase-b/privacyNotice');

process.env.NODE_ENV = 'test';
const syntheticNotice = 'Synthetic test privacy notice; not an institutional policy.';
const syntheticPolicy = 'Synthetic test public policy; not approved institutional content.';
function publishTestNotice(status = 'PUBLISHED', withdrawnAt = null) {
  memoryStore.data01TestPrivacyNotice = {
    version: 'TEST-NOTICE-1', content: syntheticNotice,
    content_hash: hashContent(syntheticNotice), publication_status: status,
    effective_at: Date.now() - 1000, withdrawn_at: withdrawnAt
  };
}
function publishTestPolicy(status = 'PUBLISHED', withdrawnAt = null) {
  memoryStore.data01TestPublicPolicy = {
    version: 'TEST-POLICY-1', content: syntheticPolicy,
    content_hash: hashContent(syntheticPolicy), publication_status: status,
    effective_at: Date.now() - 1000, withdrawn_at: withdrawnAt
  };
}

async function invoke(router, method, routePath, requestBody) {
  const layer = router.stack.find(item => item.route && item.route.path === routePath && item.route.methods[method]);
  assert(layer, `route ${method.toUpperCase()} ${routePath} exists`);
  const handler = layer.route.stack[0].handle;
  const response = {
    statusCode: 200,
    status(code) { this.statusCode = code; return this; },
    json(body) { this.body = body; return this; },
    type(value) { this.contentType = value; return this; },
    send(body) { this.body = body; return this; }
  };
  await handler({ body: requestBody || {} }, response);
  return { status: response.statusCode, json: response.body, contentType: response.contentType };
}

async function invokeAuthed(router, method, routePath, userId, requestBody = {}, params = {}) {
  const layer = router.stack.find(item => item.route && item.route.path === routePath && item.route.methods[method]);
  assert(layer, `route ${method.toUpperCase()} ${routePath} exists`);
  const handler = layer.route.stack[layer.route.stack.length - 1].handle;
  const response = { statusCode: 200, status(code) { this.statusCode = code; return this; }, json(body) { this.body = body; return this; } };
  await handler({ body: requestBody, params, user: { id: userId }, token: 'synthetic-bearer', get: () => '' }, response);
  return { status: response.statusCode, json: response.body };
}

async function run() {
    delete memoryStore.data01TestPrivacyNotice;
    const unpublishedNotice = await invoke(privacyRoutes, 'get', '/notice/current');
    assert.strictEqual(unpublishedNotice.status, 503, 'unpublished notice is unavailable');
    assert.ok(!JSON.stringify(unpublishedNotice).includes('Synthetic test privacy notice'), 'unpublished notice content is not exposed');

    publishTestNotice('DRAFT');
    const draftNotice = await invoke(privacyRoutes, 'get', '/notice/current');
    assert.strictEqual(draftNotice.status, 503, 'draft notice is not publicly served');
    assert.ok(!JSON.stringify(draftNotice).includes(syntheticNotice), 'draft notice content is not exposed');

    publishTestNotice('PUBLISHED');
    const currentPublished = await invoke(privacyRoutes, 'get', '/notice/current');
    assert.strictEqual(currentPublished.status, 200, 'published synthetic notice is served');
    assert.strictEqual(currentPublished.json.publication_status, 'PUBLISHED');
    assert.strictEqual(currentPublished.json.content, syntheticNotice);
    assert.match(currentPublished.json.sha256, /^[0-9a-f]{64}$/);
    assert.strictEqual(currentPublished.json.version, 'TEST-NOTICE-1');

    publishTestNotice('PUBLISHED', Date.now());
    const withdrawnNotice = await invoke(privacyRoutes, 'get', '/notice/current');
    assert.strictEqual(withdrawnNotice.status, 503, 'withdrawn notice is unavailable');
    assert.ok(!JSON.stringify(withdrawnNotice).includes(syntheticNotice), 'withdrawn notice content is not exposed');

    delete memoryStore.data01TestPublicPolicy;
    const noPolicy = await invoke(privacyRoutes, 'get', '/public-policy');
    assert.strictEqual(noPolicy.status, 503, 'unpublished public policy is unavailable');

    publishTestPolicy('DRAFT');
    const draftPolicy = await invoke(privacyRoutes, 'get', '/public-policy');
    assert.strictEqual(draftPolicy.status, 503, 'draft public policy is unavailable');
    assert.ok(!String(draftPolicy.json || draftPolicy.body).includes(syntheticPolicy), 'draft public policy content is not exposed');
    assert.deepStrictEqual(draftPolicy.json, noPolicy.json, 'missing and draft policy states share the same generic response');

    publishTestPolicy('PUBLISHED');
    const policy = await invoke(privacyRoutes, 'get', '/public-policy');
    assert.strictEqual(policy.status, 200, 'published synthetic policy is served');
    assert.strictEqual(policy.json, syntheticPolicy);

    publishTestPolicy('PUBLISHED', Date.now());
    const withdrawnPolicy = await invoke(privacyRoutes, 'get', '/public-policy');
    assert.strictEqual(withdrawnPolicy.status, 503, 'withdrawn public policy is unavailable');
    assert.deepStrictEqual(withdrawnPolicy.json, noPolicy.json, 'withdrawn policy state shares the same generic response');

    publishTestPolicy('PUBLISHED');
    memoryStore.data01TestPublicPolicy.content_hash = '0'.repeat(64);
    const hashMismatchPolicy = await invoke(privacyRoutes, 'get', '/public-policy');
    assert.strictEqual(hashMismatchPolicy.status, 503, 'a policy with a mismatched hash is unavailable');
    assert.deepStrictEqual(hashMismatchPolicy.json, noPolicy.json, 'hash mismatch shares the same generic response');

    const unauthorizedPublish = await invokeAuthed(privacyRoutes, 'post', '/public-policy/:version/publish', 'synthetic_non_admin',
      { reason_code: 'SYNTHETIC_TEST' }, { version: 'TEST-POLICY-1' });
    assert.strictEqual(unauthorizedPublish.status, 401, 'unauthorized actor cannot publish a policy');

    publishTestNotice('PUBLISHED');
    const current = await invoke(privacyRoutes, 'get', '/notice/current');

    const registration = { phone: '0712345678', pin: '1234', name: 'Synthetic Test Member', campus_id: 'campus_embu' };
    const denied = await invoke(authRoutes, 'post', '/register', registration);
    assert.strictEqual(denied.status, 428, 'registration without acknowledgement is rejected');
    assert.strictEqual(memoryStore.users.length, 0, 'rejected registration creates no account');

    const logNameMarker = 'SyntheticLogNameMarker';
    const logPhoneMarker = '0712345679';
    const capturedLogs = [];
    const originalConsoleError = console.error;
    console.error = (...values) => capturedLogs.push(values.join(' '));
    let malformedRegistration;
    try {
      malformedRegistration = await invoke(authRoutes, 'post', '/register', {
        phone: logPhoneMarker, pin: '8765', name: { marker: logNameMarker },
        privacy_notice_version: current.json.version,
        privacy_notice_hash: current.json.sha256,
        privacy_notice_acknowledged: true
      });
    } finally {
      console.error = originalConsoleError;
    }
    assert.strictEqual(malformedRegistration.status, 500, 'registration failure returns a generic server response');
    assert.ok(capturedLogs.length > 0, 'registration failure emits a diagnostic event');
    assert.ok(!capturedLogs.join('\n').includes(logNameMarker), 'registration logs omit synthetic name input');
    assert.ok(!capturedLogs.join('\n').includes(logPhoneMarker), 'registration logs omit synthetic phone input');
    assert.ok(!capturedLogs.join('\n').includes('8765'), 'registration logs omit synthetic PIN input');
    assert.ok(!JSON.stringify(malformedRegistration).includes(logNameMarker), 'registration response omits submitted name input');
    assert.strictEqual(memoryStore.users.length, 0, 'failed registration creates no account');

    const originalNodeEnv = process.env.NODE_ENV;
    process.env.NODE_ENV = 'production';
    const draftDenied = await invoke(authRoutes, 'post', '/register', {
      ...registration,
      privacy_notice_version: current.json.version,
      privacy_notice_hash: current.json.sha256,
      privacy_notice_presented_at: current.json.presented_at,
      privacy_notice_acknowledged: true
    });
    process.env.NODE_ENV = originalNodeEnv;
    assert.strictEqual(draftDenied.status, 503, 'production registration stays closed while the active notice is synthetic');
    assert.strictEqual(memoryStore.users.length, 0, 'draft notice cannot create a production account');

    const stale = await invoke(authRoutes, 'post', '/register', {
      ...registration,
      privacy_notice_version: '0.0', privacy_notice_hash: current.json.sha256,
      privacy_notice_presented_at: current.json.presented_at, privacy_notice_acknowledged: true
    });
    assert.strictEqual(stale.status, 428, 'a stale notice version is rejected');
    assert.strictEqual(memoryStore.users.length, 0, 'stale acknowledgement creates no account');

    const requestStartedAt = Date.now();
    const accepted = await invoke(authRoutes, 'post', '/register', {
      ...registration,
      privacy_notice_version: current.json.version,
      privacy_notice_hash: current.json.sha256,
      // This legacy field is ignored; clients cannot supply acknowledgement time.
      privacy_notice_presented_at: 'CLIENT_FORGED_TIMESTAMP',
      privacy_notice_acknowledged: true
    });
    assert.strictEqual(accepted.status, 201, 'registration with the current notice acknowledgement succeeds');
    assert.strictEqual(accepted.json.user.membership_status, 'REGISTERED', 'registration does not verify church membership');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, 1, 'acknowledgement evidence is persisted');
    const record = memoryStore.privacy_notice_acknowledgements[0];
    assert.strictEqual(record.notice_version, current.json.version);
    assert.strictEqual(record.content_hash, current.json.sha256);
    assert.ok(record.presented_at >= requestStartedAt, 'presentation timestamp is assigned by the server during registration');
    assert.ok(record.presented_at <= record.acknowledged_at, 'server presentation timestamp does not follow acknowledgement');
    assert.notStrictEqual(record.presented_at, 1, 'client-supplied timestamp is not persisted');
    assert.strictEqual(record.user_id, accepted.json.user.id);
    assert.ok(Number.isFinite(record.acknowledged_at));
    const originalAccountPhone = memoryStore.users.find(user => user.id === accepted.json.user.id).phone;
    const phoneChange = await invokeAuthed(authRoutes, 'post', '/phone-change', accepted.json.user.id, { phone: '+254700000999' });
    assert.strictEqual(phoneChange.status, 501, 'self-service phone change reports unavailable');
    assert.strictEqual(memoryStore.users.find(user => user.id === accepted.json.user.id).phone, originalAccountPhone, 'unavailable phone change leaves account number unchanged');

    // Synthetic account re-acknowledgement receipt cases. Rejections must not add evidence.
    const beforeReceiptAcks = memoryStore.privacy_notice_acknowledgements.length;
    publishTestNotice('PUBLISHED');
    const issued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    assert.strictEqual(issued.status, 200, 'authenticated account receives a server-issued presentation receipt');
    assert.ok(issued.json.presented_at <= Date.now(), 'presentation time is server-stamped');
    const wrongAccount = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_b', { receipt: issued.json.receipt });
    assert.strictEqual(wrongAccount.status, 428, 'receipt cannot be used by another account');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, beforeReceiptAcks, 'wrong-account receipt creates no acknowledgement evidence');

    const staleIssued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    publishTestNotice('PUBLISHED');
    memoryStore.data01TestPrivacyNotice.version = 'TEST-NOTICE-2';
    memoryStore.data01TestPrivacyNotice.content_hash = hashContent(syntheticNotice);
    const staleReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: staleIssued.json.receipt });
    assert.strictEqual(staleReceipt.status, 428, 'receipt for an old notice version is stale');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, beforeReceiptAcks, 'stale receipt creates no acknowledgement evidence');

    publishTestNotice('PUBLISHED');
    const wrongHashIssued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    const wrongHashRow = memoryStore.privacy_presentation_receipts[memoryStore.privacy_presentation_receipts.length - 1];
    wrongHashRow.content_hash = '0'.repeat(64);
    const wrongHashReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: wrongHashIssued.json.receipt });
    assert.strictEqual(wrongHashReceipt.status, 428, 'receipt hash must match the current document hash');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, beforeReceiptAcks, 'wrong-hash receipt creates no acknowledgement evidence');

    publishTestNotice('PUBLISHED');
    const expiredIssued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    memoryStore.privacy_presentation_receipts[memoryStore.privacy_presentation_receipts.length - 1].expires_at = Date.now() - 1;
    const expiredReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: expiredIssued.json.receipt });
    assert.strictEqual(expiredReceipt.status, 428, 'expired receipt is rejected');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, beforeReceiptAcks, 'expired receipt creates no acknowledgement evidence');

    publishTestNotice('PUBLISHED');
    const withdrawnIssued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    publishTestNotice('PUBLISHED', Date.now());
    const withdrawnReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: withdrawnIssued.json.receipt });
    assert.strictEqual(withdrawnReceipt.status, 428, 'withdrawn notice cannot be acknowledged');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, beforeReceiptAcks, 'withdrawn notice receipt creates no acknowledgement evidence');

    publishTestNotice('PUBLISHED');
    const replayIssued = await invokeAuthed(privacyRoutes, 'post', '/presentation', 'synthetic_account_a');
    const acceptedReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: replayIssued.json.receipt });
    assert.strictEqual(acceptedReceipt.status, 200, 'current single-use receipt records acknowledgement');
    const afterAcceptedReceipt = memoryStore.privacy_notice_acknowledgements.length;
    const replayReceipt = await invokeAuthed(privacyRoutes, 'post', '/acknowledge', 'synthetic_account_a', { receipt: replayIssued.json.receipt });
    assert.strictEqual(replayReceipt.status, 428, 'consumed receipt cannot be replayed');
    assert.strictEqual(memoryStore.privacy_notice_acknowledgements.length, afterAcceptedReceipt, 'replayed receipt creates no second acknowledgement');

    const narrativeMarker = 'SYNTHETIC_PRIVATE_REQUEST_NARRATIVE';
    const requestId = 'bd5e82ae-7a3b-44e5-90da-48b201ac2d21';
    const submittedRequest = await invokeAuthed(privacyRoutes, 'post', '/requests', 'synthetic_requester_a', {
      request_id: requestId, request_type: 'ACCESS', narrative: narrativeMarker
    });
    assert.strictEqual(submittedRequest.status, 202, 'rights request is accepted while acknowledgement is pending');
    assert.strictEqual(submittedRequest.json.status, 'under_review', 'requester receives a neutral status');
    assert.ok(!JSON.stringify(memoryStore.data_subject_requests).includes(narrativeMarker), 'request narrative is not stored');
    const mismatchedRequest = await invokeAuthed(privacyRoutes, 'get', '/requests/:requestId', 'synthetic_requester_b', {}, { requestId });
    assert.strictEqual(mismatchedRequest.status, 404, 'another account cannot retrieve a rights request');

    console.log('PASS privacy publication gate: unpublished, draft, published, withdrawn; registration acknowledgement binding and unverified membership');
}

run().catch(error => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});
