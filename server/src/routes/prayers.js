const express = require('express');
const { query } = require('../db');
const { optionalAuth } = require('../middleware/auth');

const router = express.Router();

// GET /api/prayers
router.get('/', async (req, res) => {
  try {
    const since = req.query.since ? parseInt(req.query.since, 10) : 0;
    const result = await query(
      'SELECT id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at FROM prayers ORDER BY created_at DESC LIMIT 100',
      [since]
    );
    return res.json({ prayers: result.rows || [] });
  } catch (err) {
    console.error('Fetch prayers error:', err);
    return res.status(500).json({ error: 'Server error loading prayers' });
  }
});

// POST /api/prayers
router.post('/', optionalAuth, async (req, res) => {
  try {
    const { id, title, content, isAnonymous, authorName } = req.body;
    if (!title || !content) {
      return res.status(400).json({ error: 'Title and prayer request content are required' });
    }

    const prayerId = id || ('prayer_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7));
    const isAnon = !!isAnonymous;
    const authorId = isAnon ? 'anonymous' : (req.user ? req.user.id : (req.body.authorId || 'guest'));
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

    return res.status(201).json({ prayer: created });
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

    await query('INSERT INTO prayer_amens (prayer_id, user_id, created_at) VALUES ($1, $2, $3)', [prayerId, userId, now]);
    await query('UPDATE prayers SET prayer_count = prayer_count + 1 WHERE id = $1', [prayerId]);

    return res.json({ success: true, prayerId });
  } catch (err) {
    console.error('Amen prayer error:', err);
    return res.status(500).json({ error: 'Server error amening prayer' });
  }
});

// POST /api/prayers/:id/testimony
router.post('/:id/testimony', optionalAuth, async (req, res) => {
  try {
    const prayerId = req.params.id;
    const { testimony } = req.body;
    const now = Date.now();

    await query('UPDATE prayers SET is_answered = true, testimony = $2, updated_at = $3 WHERE id = $1', [prayerId, testimony || '', now]);

    return res.json({ success: true, prayerId, testimony: testimony || '' });
  } catch (err) {
    console.error('Prayer testimony error:', err);
    return res.status(500).json({ error: 'Server error recording testimony' });
  }
});

module.exports = router;
