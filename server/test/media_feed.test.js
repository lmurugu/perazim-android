'use strict';
const assert = require('node:assert/strict');
const { createMediaFeedHandler, mapPlaylistItems, uploadsPlaylistId, CHANNEL_ID } = require('../src/routes/mediaFeed');

function fakeRes() {
  const r = { code: 200, body: null };
  r.status = (c) => { r.code = c; return r; };
  r.json = (b) => { r.body = b; return r; };
  return r;
}

(async () => {
  assert.equal(uploadsPlaylistId(CHANNEL_ID), 'UU2Yv5gJArnHSuXApBupkv-A');
  assert.throws(() => uploadsPlaylistId('not-a-channel'));

  const sample = { items: [
    { snippet: { title: 'Sermon A', publishedAt: '2026-11-01T00:00:00Z', resourceId: { videoId: 'abc123' }, thumbnails: { medium: { url: 'https://i.ytimg.com/a.jpg' } } } },
    { snippet: { title: 'No id' } },
  ] };
  const mapped = mapPlaylistItems(sample);
  assert.equal(mapped.length, 1);
  assert.equal(mapped[0].watchUrl, 'https://www.youtube.com/watch?v=abc123');

  // Not configured: 503, no upstream call.
  let calls = 0;
  const off = createMediaFeedHandler({ apiKey: '', fetchImpl: async () => { calls++; } });
  const r0 = fakeRes(); await off({}, r0);
  assert.equal(r0.code, 503); assert.equal(calls, 0);

  // Configured: fetches once, key sent upstream, cached on second call.
  let seenUrl = '';
  let now = 1000;
  const on = createMediaFeedHandler({ apiKey: 'TEST_KEY', now: () => now,
    fetchImpl: async (u) => { seenUrl = u; return { ok: true, json: async () => sample }; } });
  const r1 = fakeRes(); await on({}, r1);
  assert.equal(r1.code, 200); assert.equal(r1.body.items.length, 1); assert.equal(r1.body.cached, false);
  assert.ok(seenUrl.includes('playlistId=UU2Yv5gJArnHSuXApBupkv-A'));
  const r2 = fakeRes(); await on({}, r2);
  assert.equal(r2.body.cached, true);

  // Upstream failure: 502, generic message, no key or upstream body echoed.
  const bad = createMediaFeedHandler({ apiKey: 'TEST_KEY', fetchImpl: async () => ({ ok: false, json: async () => ({ error: 'TEST_KEY leaked' }) }) });
  const r3 = fakeRes(); await bad({}, r3);
  assert.equal(r3.code, 502);
  assert.ok(!JSON.stringify(r3.body).includes('TEST_KEY'));

  console.log('media_feed tests: PASS (6 checks)');
})().catch((e) => { console.error(e); process.exit(1); });
