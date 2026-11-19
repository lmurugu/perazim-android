/**
 * Member safety: blocks hide content in both directions, messages are refused across a block,
 * and reports are accepted only for content the reporter can see. Runs on a disposable PostgreSQL database.
 */
const { assertDisposableTestDatabase } = require('./test_database_safety');
if (!process.env.DATABASE_URL) {
  throw new Error('Set DATABASE_URL to a disposable test database before running content_safety.test.js');
}
assertDisposableTestDatabase(process.env.DATABASE_URL);
const crypto = require('crypto');
process.env.JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(32).toString('hex');
process.env.PORT = '3009';

const assert = require('assert');
const http = require('http');
const bcrypt = require('bcryptjs');
const { query, closeDb } = require('../src/db');
const { startServer } = require('../src/index');
const { getCurrentNotice } = require('../src/phase-b/privacyNotice');

const PORT = 3009;
let server;
let notice;

function request(method, path, token, body) {
  return new Promise((resolve, reject) => {
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const payload = body ? JSON.stringify(body) : null;
    if (payload) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(payload); }
    const req = http.request({ hostname: '127.0.0.1', port: PORT, path, method, headers }, res => {
      let data = '';
      res.on('data', chunk => (data += chunk));
      res.on('end', () => {
        let json = data;
        try { json = JSON.parse(data); } catch (_) { /* keep text */ }
        resolve({ status: res.statusCode, body: json });
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function member(label, role = 'MEMBER') {
  const phone = `+2547${crypto.randomInt(0, 100000000).toString().padStart(8, '0')}`;
  const pin = String(100000 + crypto.randomInt(900000));
  const reg = await request('POST', '/api/auth/register', null, {
    phone, pin, name: `Synthetic ${label}`, campus_id: 'campus_embu', role, ...notice
  });
  assert.ok(reg.status === 201 || reg.status === 200 || reg.status === 409, `register ${label}: ${JSON.stringify(reg.body)}`);
  await query("UPDATE users SET pin_hash = $1, role = $2 WHERE phone = $3", [bcrypt.hashSync(pin, 10), role, phone]);
  const login = await request('POST', '/api/auth/login', null, { phone, pin });
  assert.strictEqual(login.status, 200, `login ${label}`);
  return { token: login.body.token, id: login.body.user.id, name: login.body.user.name };
}

async function run() {
  server = await startServer();
  try {
    notice = await getCurrentNotice().then(n => ({
      privacy_notice_version: n.version, privacy_notice_hash: n.content_hash,
      privacy_notice_acknowledged: true, age_confirmed: true
    }));
    const admin = await member('Admin', 'ADMIN');
    const a = await member('Author');
    const b = await member('Reader');
    const c = await member('Bystander');

    const postA = await request('POST', '/api/community/posts', a.token, { type: 'GENERAL', title: 'Author post', content: 'Blessed Sunday to all.' });
    assert.strictEqual(postA.status, 201);
    const postB = await request('POST', '/api/community/posts', b.token, { type: 'GENERAL', title: 'Reader post', content: 'Praise the Lord.' });
    assert.strictEqual(postB.status, 201);
    const commentA = await request('POST', `/api/community/posts/${postA.body.post.id}/comments`, a.token, { content: 'Amen from the author.' });
    assert.strictEqual(commentA.status, 201);
    const commentB = await request('POST', `/api/community/posts/${postA.body.post.id}/comments`, b.token, { content: 'Amen from the reader.' });
    assert.strictEqual(commentB.status, 201);
    console.log('PASS: setup posts and comments');

    // Reports
    const selfReport = await request('POST', '/api/community/reports', a.token, { targetType: 'POST', targetId: postA.body.post.id, reason: 'SPAM' });
    assert.strictEqual(selfReport.status, 400, 'cannot report own content');
    const badReason = await request('POST', '/api/community/reports', b.token, { targetType: 'POST', targetId: postA.body.post.id, reason: 'RUDE' });
    assert.strictEqual(badReason.status, 400, 'reason must be from the list');
    const report = await request('POST', '/api/community/reports', b.token, { targetType: 'POST', targetId: postA.body.post.id, reason: 'harassment', details: 'Unkind words' });
    assert.strictEqual(report.status, 201, JSON.stringify(report.body));
    const reportComment = await request('POST', '/api/community/reports', c.token, { targetType: 'COMMENT', targetId: commentB.body.comment.id, reason: 'HATE' });
    assert.strictEqual(reportComment.status, 201, 'a visible comment can be reported');
    const hiddenPost = await request('POST', '/api/community/reports', c.token, { targetType: 'POST', targetId: 'missing_post', reason: 'SPAM' });
    assert.strictEqual(hiddenPost.status, 404, 'unknown content is not reportable');
    console.log('PASS: reports are validated and accepted for visible content');

    // Block: Reader blocks Author
    const block = await request('POST', '/api/community/blocks', b.token, { userId: a.id });
    assert.strictEqual(block.status, 201);
    const selfBlock = await request('POST', '/api/community/blocks', b.token, { userId: b.id });
    assert.strictEqual(selfBlock.status, 400);

    const readerFeed = await request('GET', '/api/community/posts', b.token);
    assert.ok(!readerFeed.body.posts.some(p => p.author_id === a.id), 'blocked author posts hidden from blocker');
    const authorFeed = await request('GET', '/api/community/posts', a.token);
    assert.ok(!authorFeed.body.posts.some(p => p.author_id === b.id), 'blocker posts hidden from blocked author');
    assert.ok(authorFeed.body.posts.some(p => p.author_id === a.id), 'own posts still visible');
    const readerComments = await request('GET', `/api/community/posts/${postA.body.post.id}/comments`, b.token);
    assert.ok(!readerComments.body.comments.some(x => x.author_id === a.id), 'blocked author comments hidden');
    const bystanderComments = await request('GET', `/api/community/posts/${postA.body.post.id}/comments`, c.token);
    assert.ok(bystanderComments.body.comments.some(x => x.author_id === a.id), 'other members still see comments');
    console.log('PASS: block hides posts and comments in both directions');

    const toBlocked = await request('POST', '/api/community/messages', b.token, { receiverId: a.id, content: 'Hello' });
    assert.strictEqual(toBlocked.status, 403, 'blocker cannot message');
    const fromBlocked = await request('POST', '/api/community/messages', a.token, { receiverId: b.id, content: 'Hello' });
    assert.strictEqual(fromBlocked.status, 403, 'blocked author cannot message');
    const convs = await request('GET', '/api/community/conversations', b.token);
    assert.ok(!convs.body.conversations.some(x => x.partner_id === a.id));
    const blockList = await request('GET', '/api/community/blocks', b.token);
    assert.ok(blockList.body.blocks.some(x => x.user_id === a.id));
    console.log('PASS: messages refused across a block');

    // Message reports only for participants
    const msgToC = await request('POST', '/api/community/messages', a.token, { receiverId: c.id, content: 'Private note' });
    assert.strictEqual(msgToC.status, 201);
    const strangerReport = await request('POST', '/api/community/reports', b.token, { targetType: 'MESSAGE', targetId: msgToC.body.message.id, reason: 'PRIVACY' });
    assert.strictEqual(strangerReport.status, 404, 'non-participants cannot report a message');
    const memberReport = await request('POST', '/api/community/reports', c.token, { targetType: 'MEMBER', targetId: a.id, reason: 'SPAM' });
    assert.strictEqual(memberReport.status, 201);

    // Admin review
    const notAdmin = await request('GET', '/api/community/reports', b.token);
    assert.strictEqual(notAdmin.status, 403);
    const open = await request('GET', '/api/community/reports', admin.token);
    assert.strictEqual(open.status, 200);
    assert.ok(open.body.reports.length >= 3);
    const decided = await request('POST', `/api/community/reports/${report.body.id}/decision`, admin.token, { status: 'ACTIONED' });
    assert.strictEqual(decided.status, 200);
    const badDecision = await request('POST', `/api/community/reports/${report.body.id}/decision`, admin.token, { status: 'MAYBE' });
    assert.strictEqual(badDecision.status, 400);
    console.log('PASS: reports are reviewable by administrators only');

    // Unblock restores visibility
    const unblock = await request('DELETE', `/api/community/blocks/${a.id}`, b.token);
    assert.strictEqual(unblock.status, 200);
    const restored = await request('GET', '/api/community/posts', b.token);
    assert.ok(restored.body.posts.some(p => p.author_id === a.id), 'unblocking restores content');
    const sendAgain = await request('POST', '/api/community/messages', b.token, { receiverId: a.id, content: 'Back again' });
    assert.strictEqual(sendAgain.status, 201);
    console.log('PASS: unblocking restores content and messaging');
    console.log('ALL CONTENT SAFETY TESTS PASSED');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    await closeDb();
  }
}

run().catch(err => {
  console.error('CONTENT SAFETY TEST FAILED:', err);
  process.exit(1);
});
