const express = require('express');
const { query, withTransaction } = require('../db');
const { optionalAuth } = require('../middleware/auth');

const router = express.Router();
const { randomUUID } = require('crypto');

function parsePayload(payloadJson) {
  if (!payloadJson) return {};
  if (typeof payloadJson === 'object') return payloadJson;
  return JSON.parse(payloadJson);
}

async function applyMutation(client, mutation, payload, user, now, requestBody = {}) {
  const { id, operationType, entityType, entityId } = mutation;
  const actorClaims = [
    ['userId', requestBody.userId], ['authorId', requestBody.authorId], ['amenUserId', requestBody.amenUserId],
    ['userId', payload.userId], ['authorId', payload.authorId], ['amenUserId', payload.amenUserId]
  ];
  const mismatch = actorClaims.find(([, claimedId]) => claimedId != null && String(claimedId) !== user.id);
  if (mismatch) return { ok: false, reason: 'ACTOR_MISMATCH', message: `${mismatch[0]} must match the authenticated account` };

  if (entityType === 'PRAYER' && operationType === 'CREATE') {
    const prayerId = entityId || id;
    const title = payload.title || 'Prayer Request';
    const content = payload.content || '';
    const isAnon = !!payload.isAnonymous;
    const existingRes = await client.query('SELECT id, author_id, content FROM prayers WHERE id = $1', [prayerId]);
    const existing = existingRes.rows && existingRes.rows[0];
    if (existing && existing.author_id !== user.id) {
      return { ok: false, reason: 'CONFLICT_MISMATCH', message: 'Prayer ID is registered to another author' };
    }
    if (existing && existing.content !== content) {
      return { ok: false, reason: 'CONFLICT_MISMATCH', message: 'Prayer ID already exists with conflicting content' };
    }
    if (!existing) {
      await client.query(
        'INSERT INTO prayers (id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)',
        [prayerId, title, content, user.id, isAnon ? 'Anonymous Saint' : user.name, isAnon, 'PUBLIC', 0, false, '', now, now]
      );
    }
    return { ok: true };
  }

  if (entityType === 'PRAYER_AMEN' && operationType === 'CREATE') {
    const prayerId = entityId;
    const prayer = await client.query('SELECT id FROM prayers WHERE id = $1', [prayerId]);
    if (!prayer.rows || !prayer.rows[0]) return { ok: false, reason: 'NOT_FOUND', message: 'Prayer request not found' };
    const inserted = await client.query(
      'INSERT INTO prayer_amens (prayer_id, user_id, created_at) VALUES ($1, $2, $3) ON CONFLICT (prayer_id, user_id) DO NOTHING',
      [prayerId, user.id, now]
    );
    if (inserted.rowCount > 0) await client.query('UPDATE prayers SET prayer_count = prayer_count + 1 WHERE id = $1', [prayerId]);
    return { ok: true };
  }

  if (entityType === 'PRAYER_TESTIMONY' && operationType === 'UPDATE') {
    const prayerId = entityId;
    const prayerRes = await client.query('SELECT id, author_id FROM prayers WHERE id = $1', [prayerId]);
    const prayer = prayerRes.rows && prayerRes.rows[0];
    if (!prayer) return { ok: false, reason: 'NOT_FOUND', message: 'Prayer request not found' };
    if (prayer.author_id !== user.id && user.role !== 'ADMIN') {
      return { ok: false, reason: 'FORBIDDEN', message: 'Only original prayer author may record testimony' };
    }
    await client.query('UPDATE prayers SET is_answered = true, testimony = $2, updated_at = $3 WHERE id = $1', [prayerId, payload.testimony || '', now]);
    return { ok: true };
  }

  if (entityType === 'USER_PROGRESS' && operationType === 'UPDATE') {
    if (payload.xp !== undefined) {
      await client.query('UPDATE users SET xp = $1, streak = $2, updated_at = $3 WHERE id = $4', [payload.xp, payload.streak || 0, now, user.id]);
    }
    return { ok: true };
  }
  return { ok: false, reason: 'UNSUPPORTED_OPERATION', message: `Entity '${entityType}' or operation '${operationType}' is not supported by this server` };
}

function sanitizePrayer(prayer) {
  if (prayer && (prayer.is_anonymous || prayer.is_anonymous === 'true' || prayer.is_anonymous === 1)) {
    return {
      ...prayer,
      author_id: 'anonymous',
      author_name: prayer.author_name || 'Anonymous Saint'
    };
  }
  return prayer;
}

// POST /api/sync
router.post('/', optionalAuth, async (req, res) => {
  try {
    const { mutations, lastSyncTimestamp, cursor, limit: reqLimit } = req.body || {};
    if (Array.isArray(mutations) && mutations.length > 0 && !req.user) {
      return res.status(401).json({ error: 'Authentication required for sync mutations' });
    }
    const userId = req.user ? req.user.id : null;
    const syncedIds = [];
    const rejectedMutations = [];
    const now = Date.now();

    if (Array.isArray(mutations)) {
      for (const m of mutations) {
        try {
          const { id, operationType, entityType, entityId, payloadJson } = m;
          const payload = parsePayload(payloadJson);
          const outcome = await withTransaction(async (client) => {
            // Serialize identical user/mutation keys so concurrent retries see the first receipt.
            await client.query('SELECT pg_advisory_xact_lock(hashtextextended($1, 0))', [`${userId}:${id}`]);
            const prior = await client.query(
              'SELECT response_json FROM sync_receipts WHERE user_id = $1 AND mutation_id = $2', [userId, id]
            );
            if (prior.rows && prior.rows[0]) {
              const stored = prior.rows[0].response_json;
              return typeof stored === 'string' ? JSON.parse(stored) : stored;
            }

            const result = await applyMutation(client, m, payload, req.user, now, req.body || {});
            await client.query(
              'INSERT INTO sync_events (id, user_id, entity_type, entity_id, operation_type, payload, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)',
              [randomUUID(), userId, entityType, entityId || id, operationType, payloadJson || '', now]
            );
            const response = result.ok ? { ok: true } : { ok: false, reason: result.reason, message: result.message };
            await client.query(
              'INSERT INTO sync_receipts (user_id, mutation_id, status, response_json, created_at) VALUES ($1, $2, $3, $4, $5) ON CONFLICT (user_id, mutation_id) DO NOTHING',
              [userId, id, response.ok ? 'SYNCED' : 'REJECTED', JSON.stringify(response), now]
            );
            return response;
          });
          if (outcome && outcome.ok) syncedIds.push(id);
          else rejectedMutations.push({ id, reason: outcome.reason, message: outcome.message });
        } catch (itemErr) {
          console.error('Sync mutation transaction rolled back:', itemErr.message);
          // The client receives PROCESSING_ERROR; business data, sync_events, and
          // the receipt share this transaction, so retrying reprocesses the mutation.
          rejectedMutations.push({ id: m && m.id, reason: 'PROCESSING_ERROR', message: 'Mutation was not applied; retry is safe' });
        }
      }
    }

    // REQ-07: Paginated Synchronization with stable ordering (updated_at ASC, id ASC)
    const limit = Math.min(Math.max(1, parseInt(reqLimit || req.body.limit || req.query.limit, 10) || 50), 100);
    const fetchLimit = limit + 1;

    let cursorTime = 0;
    let cursorId = null;

    const rawCursor = cursor !== undefined && cursor !== null && cursor !== '' 
      ? cursor 
      : (req.query.cursor !== undefined && req.query.cursor !== null && req.query.cursor !== '' ? req.query.cursor : (lastSyncTimestamp !== undefined && lastSyncTimestamp !== null && lastSyncTimestamp !== '' ? lastSyncTimestamp : req.body.lastSyncTimestamp));

    if (rawCursor !== undefined && rawCursor !== null && rawCursor !== '') {
      if (typeof rawCursor === 'number') {
        cursorTime = rawCursor;
      } else {
        const cursorStr = String(rawCursor).trim();
        if (cursorStr.includes(':')) {
          const parts = cursorStr.split(':');
          cursorTime = parseInt(parts[0], 10) || 0;
          cursorId = parts.slice(1).join(':') || null;
        } else {
          const parsed = parseInt(cursorStr, 10);
          cursorTime = !isNaN(parsed) ? parsed : 0;
        }
      }
    }

    const readUserId = req.user ? req.user.id : null;
    // Keep this aligned with the existing role-based admin check used for testimony writes.
    const readIsAdmin = !!(req.user && req.user.role === 'ADMIN');
    const visiblePrayer = "(visibility = 'PUBLIC' OR ($1::text IS NOT NULL AND author_id = $1) OR $2::boolean)";
    let prayersResult;
    if (cursorId) {
      prayersResult = await query(
        `SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at
           FROM prayers WHERE ${visiblePrayer} AND (updated_at, id) > ($3, $4)
           ORDER BY updated_at ASC, id ASC LIMIT $5`,
        [readUserId, readIsAdmin, cursorTime, cursorId, fetchLimit]
      );
    } else if (cursorTime > 0) {
      prayersResult = await query(
        `SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at
           FROM prayers WHERE ${visiblePrayer} AND (updated_at > $3 OR created_at > $3)
           ORDER BY updated_at ASC, id ASC LIMIT $4`,
        [readUserId, readIsAdmin, cursorTime, fetchLimit]
      );
    } else {
      prayersResult = await query(
        `SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at
           FROM prayers WHERE ${visiblePrayer} AND updated_at >= $3
           ORDER BY updated_at ASC, id ASC LIMIT $4`,
        [readUserId, readIsAdmin, 0, fetchLimit]
      );
    }

    const fetchedRows = prayersResult.rows || [];
    const hasMore = fetchedRows.length > limit;
    const pagedPrayers = hasMore ? fetchedRows.slice(0, limit) : fetchedRows;
    const lastPrayer = pagedPrayers.length > 0 ? pagedPrayers[pagedPrayers.length - 1] : null;
    const nextCursor = (hasMore && lastPrayer) ? `${lastPrayer.updated_at}:${lastPrayer.id}` : null;

    // REQ-06: Anonymous Privacy - mask author_id as 'anonymous'
    const sanitizedPrayers = pagedPrayers.map(sanitizePrayer);

    return res.json({
      syncedIds,
      rejectedMutations,
      serverTime: now,
      hasMore,
      nextCursor,
      prayers: sanitizedPrayers,
      updatedPrayers: sanitizedPrayers
    });
  } catch (err) {
    console.error('Batch sync error:', err);
    return res.status(500).json({ error: 'Server error during batch sync' });
  }
});

module.exports = router;
