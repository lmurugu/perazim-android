'use strict';

const assert = require('node:assert/strict');
const state = { prayers: [], syncEvents: [] };
const dbPath = require.resolve('../src/db');
require.cache[dbPath] = {
  id: dbPath,
  filename: dbPath,
  loaded: true,
  exports: {
    query: async (sql, params = []) => {
      const normalized = sql.replace(/\s+/g, ' ').trim().toLowerCase();
      if (normalized.startsWith('select id, author_id, content from prayers where id = $1')) {
        const prayer = state.prayers.find(row => row.id === params[0]);
        return { rows: prayer ? [{ id: prayer.id, author_id: prayer.author_id, content: prayer.content }] : [] };
      }
      if (normalized.startsWith('select id, author_id from prayers where id = $1')) {
        const prayer = state.prayers.find(row => row.id === params[0]);
        return { rows: prayer ? [{ id: prayer.id, author_id: prayer.author_id }] : [] };
      }
      if (normalized.startsWith('select id, title, content, author_id, author_name')) {
        return { rows: [...state.prayers] };
      }
      if (normalized.startsWith('insert into prayers')) {
        const [id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at] = params;
        const prayer = { id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at };
        state.prayers.push(prayer);
        return { rows: [prayer], rowCount: 1 };
      }
      if (normalized.startsWith('update prayers set is_answered = true')) {
        const prayer = state.prayers.find(row => row.id === params[0]);
        if (prayer) Object.assign(prayer, { is_answered: true, testimony: params[1], updated_at: params[2] });
        return { rows: prayer ? [prayer] : [], rowCount: prayer ? 1 : 0 };
      }
      if (normalized.startsWith('insert into sync_events')) {
        const [id, user_id, entity_type, entity_id, operation_type, payload, created_at] = params;
        state.syncEvents.push({ id, user_id, entity_type, entity_id, operation_type, payload, created_at });
        return { rows: [], rowCount: 1 };
      }
      throw new Error(`Unexpected test query: ${normalized}`);
    },
    withTransaction: async callback => callback({ query: async () => ({ rows: [], rowCount: 0 }) })
  }
};

const prayers = require('../src/routes/prayers');
const sync = require('../src/routes/sync');
const auth = require('../src/middleware/auth');

async function invoke(router, method, routePath, req) {
  const layer = router.stack.find(item => item.route && item.route.path === routePath && item.route.methods[method]);
  assert(layer, `${method.toUpperCase()} ${routePath} exists`);
  const handler = layer.route.stack[layer.route.stack.length - 1].handle;
  const res = {
    statusCode: 200,
    body: null,
    status(code) { this.statusCode = code; return this; },
    json(body) { this.body = body; return this; }
  };
  await handler({ body: {}, query: {}, params: {}, headers: {}, ...req }, res);
  return res;
}

async function run() {
  const memberA = { id: 'synthetic_member_a', name: 'Synthetic A', role: 'MEMBER' };
  const memberB = { id: 'synthetic_member_b', name: 'Synthetic B', role: 'MEMBER' };

  const authenticated = await invoke(prayers, 'post', '/', {
    user: memberA,
    body: { id: 'auth_prayer', title: 'Member prayer', content: 'Synthetic content', authorId: memberB.id }
  });
  assert.equal(authenticated.statusCode, 201);
  assert.equal(state.prayers.find(row => row.id === 'auth_prayer').author_id, memberA.id,
    'authenticated ownership comes from validated server session context');

  const guest = await invoke(prayers, 'post', '/', {
    body: { id: 'guest_prayer', title: 'Guest prayer', content: 'Synthetic guest content', authorId: memberA.id }
  });
  assert.equal(guest.statusCode, 201, 'public guest submission remains enabled');
  assert.equal(state.prayers.find(row => row.id === 'guest_prayer').author_id, 'guest',
    'a guest cannot claim an existing account ID');

  const invalidIdentityReq = { headers: { authorization: 'Bearer invalid.token.value' } };
  await new Promise(resolve => auth.optionalAuth(invalidIdentityReq, {}, resolve));
  assert.equal(invalidIdentityReq.user, undefined, 'invalid bearer identity remains unauthenticated');
  const invalidIdentityPrayer = await invoke(prayers, 'post', '/', {
    headers: invalidIdentityReq.headers,
    body: { id: 'invalid_identity_prayer', title: 'Invalid token guest prayer', content: 'Synthetic content' }
  });
  assert.equal(invalidIdentityPrayer.statusCode, 201);
  assert.equal(state.prayers.find(row => row.id === 'invalid_identity_prayer').author_id, 'guest',
    'missing or invalid identity is represented as a server-owned guest');

  const syncGuest = await invoke(sync, 'post', '/', {
    body: {
      userId: memberA.id,
      mutations: [{ id: 'guest_sync_item', operationType: 'CREATE', entityType: 'PRAYER', entityId: 'guest_sync_prayer',
        payloadJson: JSON.stringify({ title: 'Guest offline prayer', content: 'Synthetic offline content', authorId: memberA.id }) }]
    }
  });
  assert.equal(syncGuest.statusCode, 200);
  assert.deepEqual(syncGuest.body.syncedIds, ['guest_sync_item']);
  assert.equal(state.prayers.find(row => row.id === 'guest_sync_prayer').author_id, 'guest',
    'offline guest authorId is ignored');
  assert.equal(state.syncEvents.find(row => row.entity_id === 'guest_sync_prayer').user_id, 'guest',
    'offline guest userId is ignored for audit attribution');

  const forgedReplay = await invoke(sync, 'post', '/', {
    body: {
      userId: memberA.id,
      mutations: [{ id: 'forged_replay', operationType: 'CREATE', entityType: 'PRAYER', entityId: 'auth_prayer',
        payloadJson: JSON.stringify({ title: 'Member prayer', content: 'Synthetic content', authorId: memberA.id }) }]
    }
  });
  assert.equal(forgedReplay.body.syncedIds.includes('forged_replay'), false);
  assert.equal(forgedReplay.body.rejectedMutations[0].reason, 'CONFLICT_MISMATCH',
    'guest replay cannot claim a member prayer using a forged identity');

  const deniedTestimony = await invoke(prayers, 'post', '/:id/testimony', {
    user: memberB,
    params: { id: 'auth_prayer' },
    body: { testimony: 'Synthetic unauthorized change' }
  });
  assert.equal(deniedTestimony.statusCode, 403, 'another member cannot modify the prayer through testimony');
  assert.equal(state.prayers.find(row => row.id === 'auth_prayer').testimony, '',
    'denied cross-account modification leaves the prayer unchanged');

  const ownerTestimony = await invoke(prayers, 'post', '/:id/testimony', {
    user: memberA,
    params: { id: 'auth_prayer' },
    body: { testimony: 'Synthetic owner update' }
  });
  assert.equal(ownerTestimony.statusCode, 200, 'authenticated owner can update their prayer');
  assert.equal(state.prayers.find(row => row.id === 'auth_prayer').testimony, 'Synthetic owner update');

  process.stdout.write('PASS: prayer guest identity and ownership handler regressions (synthetic in-memory query adapter; no socket or PostgreSQL persistence).\n');
}

run().catch(error => { console.error(error); process.exitCode = 1; });
