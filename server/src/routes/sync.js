const express = require('express');
const { query } = require('../db');
const { optionalAuth } = require('../middleware/auth');

const router = express.Router();

// POST /api/sync
router.post('/', optionalAuth, async (req, res) => {
  try {
    const { mutations, lastSyncTimestamp } = req.body;
    const userId = req.user ? req.user.id : (req.body.userId || 'guest');
    const syncedIds = [];
    const now = Date.now();

    if (Array.isArray(mutations)) {
      for (const m of mutations) {
        try {
          const { id, operationType, entityType, entityId, payloadJson } = m;
          let payload = {};
          try {
            payload = payloadJson ? JSON.parse(payloadJson) : {};
          } catch (e) {
            payload = {};
          }

          if (entityType === 'PRAYER' && operationType === 'CREATE') {
            const prayerId = entityId || id;
            const title = payload.title || 'Prayer Request';
            const content = payload.content || '';
            const isAnon = !!payload.isAnonymous;
            const authorId = isAnon ? 'anonymous' : userId;
            const authorName = isAnon ? 'Anonymous Saint' : (req.user ? req.user.name : (payload.authorName || 'Fellow Saint'));

            await query(
              'INSERT INTO prayers (id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)',
              [prayerId, title, content, authorId, authorName, isAnon, 'PUBLIC', 0, false, '', now, now]
            );
            syncedIds.push(id);
          } else if (entityType === 'PRAYER_AMEN') {
            const prayerId = entityId;
            await query('INSERT INTO prayer_amens (prayer_id, user_id, created_at) VALUES ($1, $2, $3)', [prayerId, userId, now]);
            await query('UPDATE prayers SET prayer_count = prayer_count + 1 WHERE id = $1', [prayerId]);
            syncedIds.push(id);
          } else if (entityType === 'PRAYER_TESTIMONY') {
            const prayerId = entityId;
            const testimony = payload.testimony || '';
            await query('UPDATE prayers SET is_answered = true, testimony = $2, updated_at = $3 WHERE id = $1', [prayerId, testimony, now]);
            syncedIds.push(id);
          } else if (entityType === 'USER_PROGRESS') {
            if (req.user && payload.xp !== undefined) {
              await query('UPDATE users SET xp = $1, streak = $2, updated_at = $3 WHERE id = $4', [payload.xp, payload.streak || 0, now, req.user.id]);
            }
            syncedIds.push(id);
          } else {
            // Default accept for generic enqueued events
            syncedIds.push(id);
          }

          // Record sync audit
          await query(
            'INSERT INTO sync_events (id, user_id, entity_type, entity_id, operation_type, payload, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)',
            ['sync_' + now + '_' + Math.random().toString(36).substring(2, 6), userId, entityType, entityId, operationType, payloadJson || '', now]
          );
        } catch (itemErr) {
          console.warn('Error processing mutation item:', itemErr.message);
        }
      }
    }

    // Return new remote prayers updated since lastSyncTimestamp
    const since = lastSyncTimestamp ? parseInt(lastSyncTimestamp, 10) : 0;
    const prayersResult = await query(
      'SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at FROM prayers ORDER BY created_at DESC LIMIT 50',
      [since]
    );

    return res.json({
      syncedIds,
      serverTime: now,
      prayers: prayersResult.rows || []
    });
  } catch (err) {
    console.error('Batch sync error:', err);
    return res.status(500).json({ error: 'Server error during batch sync' });
  }
});

module.exports = router;
