const express = require('express');
const { query, withTransaction } = require('../db');
const { requireAuth, optionalAuth } = require('../middleware/auth');

const router = express.Router();

// Helper to sanitize anonymous prayer representations
function sanitizePrayer(prayer) {
  if (prayer && prayer.is_anonymous) {
    return {
      ...prayer,
      author_id: 'anonymous',
      author_name: prayer.author_name || 'Anonymous Saint'
    };
  }
  return prayer;
}

// GET /api/prayers
router.get('/', async (req, res) => {
  try {
    const rawSince = req.query.since || req.query.cursor;
    const since = (rawSince && !isNaN(rawSince)) ? Math.max(0, parseInt(rawSince, 10)) : 0;
    const result = await query(
      'SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at FROM prayers WHERE updated_at > $1 OR created_at > $1 ORDER BY created_at DESC LIMIT 100',
      [since]
    );
    const prayers = (result.rows || []).map(sanitizePrayer);
    return res.json({ prayers });
  } catch (err) {
    console.error('Fetch prayers error:', err);
    return res.status(500).json({ error: 'Server error loading prayers' });
  }
});

// POST /api/prayers
router.post('/', optionalAuth, async (req, res) => {
  try {
    const { id, title, content, isAnonymous, authorName } = req.body;
    if (!title || !content || typeof title !== 'string' || typeof content !== 'string' || !title.trim() || !content.trim()) {
      return res.status(400).json({ error: 'Title and prayer request content are required' });
    }

    const prayerId = id || ('prayer_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7));
    const isAnon = !!isAnonymous;
    const authorId = req.user ? req.user.id : (req.body.authorId || 'guest');
    const effAuthorName = isAnon ? 'Anonymous Saint' : (req.user ? req.user.name : (authorName || 'Fellow Saint'));
    const now = Date.now();

    const result = await query(
      'INSERT INTO prayers (id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) RETURNING *',
      [prayerId, title.trim(), content.trim(), authorId, effAuthorName, isAnon, 'PUBLIC', 0, false, '', now, now]
    );

    const created = result.rows && result.rows[0] ? result.rows[0] : {
      id: prayerId,
      title: title.trim(),
      content: content.trim(),
      author_id: authorId,
      author_name: effAuthorName,
      is_anonymous: isAnon,
      visibility: 'PUBLIC',
      prayer_count: 0,
      is_answered: false,
      testimony: '',
      created_at: now,
      updated_at: now
    };

    return res.status(201).json({ prayer: sanitizePrayer(created) });
  } catch (err) {
    console.error('Create prayer error:', err);
    return res.status(500).json({ error: 'Server error submitting prayer' });
  }
});

// POST /api/prayers/:id/amen
router.post('/:id/amen', optionalAuth, async (req, res) => {
  try {
    const prayerId = req.params.id;
    const userId = req.user ? req.user.id : (req.body.userId || 'anonymous');
    const now = Date.now();

    const result = await withTransaction(async (client) => {
      const insertRes = await client.query(
        'INSERT INTO prayer_amens (prayer_id, user_id, created_at) VALUES ($1, $2, $3) ON CONFLICT (prayer_id, user_id) DO NOTHING',
        [prayerId, userId, now]
      );

      // Only increment prayer_count if a new amen record was inserted
      let updated = false;
      if (insertRes && insertRes.rowCount > 0) {
        await client.query('UPDATE prayers SET prayer_count = prayer_count + 1 WHERE id = $1', [prayerId]);
        updated = true;
      }
      return { inserted: insertRes ? insertRes.rowCount > 0 : false, updated };
    });

    return res.json({ success: true, prayerId, inserted: result.inserted });
  } catch (err) {
    console.error('Amen prayer error:', err);
    return res.status(500).json({ error: 'Server error amening prayer' });
  }
});

// POST /api/prayers/:id/testimony
router.post('/:id/testimony', requireAuth, async (req, res) => {
  try {
    const prayerId = req.params.id;
    const { testimony } = req.body;
    if (!testimony || typeof testimony !== 'string' || !testimony.trim()) {
      return res.status(400).json({ error: 'Testimony content is required' });
    }
    const now = Date.now();

    const check = await query('SELECT id, author_id FROM prayers WHERE id = $1', [prayerId]);
    const prayer = check.rows && check.rows[0];
    if (!prayer) {
      return res.status(404).json({ error: 'Prayer not found' });
    }

    if (prayer.author_id !== req.user.id && req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Only the author may record a testimony for this prayer' });
    }

    await query('UPDATE prayers SET is_answered = true, testimony = $2, updated_at = $3 WHERE id = $1', [prayerId, testimony.trim(), now]);

    return res.json({ success: true, prayerId, testimony: testimony.trim() });
  } catch (err) {
    console.error('Prayer testimony error:', err);
    return res.status(500).json({ error: 'Server error recording testimony' });
  }
});

router.sanitizePrayer = sanitizePrayer;
module.exports = router;
