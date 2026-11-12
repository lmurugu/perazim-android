const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { query, withTransaction, isPostgresActive } = require('../db');
const { requireAuth, optionalAuth } = require('../middleware/auth');
const { validateAndSanitizeImage } = require('../utils/mediaValidator');

const UPLOADS_DIR = path.join(__dirname, '../../uploads');
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

// Group contents and rosters are private to current members (or the church admin).
async function canAccessGroup(user, groupId) {
  if (!user) return false;
  const result = await query(`SELECT g.id FROM community_groups g
    WHERE g.id = $1 AND g.is_active = TRUE AND
    ($2 = 'ADMIN' OR g.created_by = $3 OR EXISTS
      (SELECT 1 FROM group_memberships m WHERE m.group_id = g.id AND m.user_id = $3))`,
    [groupId, user.role, user.id]);
  return result.rows.length > 0;
}
async function canManageGroup(user, groupId) {
  const result = await query(`SELECT id FROM community_groups
    WHERE id = $1 AND is_active = TRUE AND (created_by = $2 OR $3 = 'ADMIN')`,
    [groupId, user.id, user.role]);
  return result.rows.length > 0;
}
async function canAccessPost(user, postId) {
  const result = await query('SELECT group_id FROM community_posts WHERE id = $1', [postId]);
  if (!result.rows.length) return false;
  return !result.rows[0].group_id || await canAccessGroup(user, result.rows[0].group_id);
}

// Only local, validated uploads owned by this member can be attached.
async function ownsMedia(userId, mediaUrl) {
  if (!mediaUrl) return true;
  if (typeof mediaUrl !== 'string' || !/^\/uploads\/att_[a-zA-Z0-9_.-]+$/.test(mediaUrl)) return false;
  const result = await query('SELECT 1 FROM community_media WHERE filename = $1 AND owner_id = $2', [path.basename(mediaUrl), userId]);
  return result.rows.length > 0;
}

// Used for the legacy /uploads path too: never expose private photos as static files.
const serveMedia = [optionalAuth, async (req, res) => {
  try {
    const filename = req.params.filename;
    if (!/^att_[a-zA-Z0-9_.-]+$/.test(filename)) return res.sendStatus(404);
    const url = '/uploads/' + filename;
    const posts = await query('SELECT id FROM community_posts WHERE media_url = $1', [url]);
    let allowed = false;
    for (const post of posts.rows) if (await canAccessPost(req.user, post.id)) { allowed = true; break; }
    if (!allowed && req.user) {
      const mine = await query('SELECT 1 FROM community_media WHERE filename = $1 AND owner_id = $2', [filename, req.user.id]);
      const chats = await query('SELECT 1 FROM messages WHERE media_url = $1 AND (sender_id = $2 OR receiver_id = $2)', [url, req.user.id]);
      // Profile photos are visible in the authenticated member directory. Keep
      // that visibility explicit and scoped to the exact URL stored on a user.
      const directoryAvatar = await query('SELECT 1 FROM users WHERE avatar_url = $1 LIMIT 1', [url]);
      allowed = mine.rows.length > 0 || chats.rows.length > 0 || directoryAvatar.rows.length > 0;
    }
    if (!allowed) return res.sendStatus(404);
    res.set('Cache-Control', 'private, no-store');
    return res.sendFile(path.join(UPLOADS_DIR, filename));
  } catch (error) { return res.sendStatus(500); }
}];

// Helper to get current date string in Africa/Nairobi timezone (YYYY-MM-DD)
function getNairobiDateString() {
  const d = new Date();
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Africa/Nairobi',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
  return formatter.format(d);
}

// Ensure default groups and daily challenge exist
async function ensureSeedData() {
  try {
    // Seed only for an administrator explicitly provisioned by the operator.
    // Phone ownership must never grant administrative privileges.
    if (!isPostgresActive()) return;
    const adminUser = await query("SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1");
    if (adminUser.rows.length === 0) return;
    const creatorId = adminUser.rows[0].id;

    const groupsRes = await query("SELECT COUNT(*) FROM community_groups");
    if (parseInt(groupsRes.rows[0].count, 10) === 0) {
      const now = Date.now();
      await query(
        "INSERT INTO community_groups (id, name, description, created_by, is_active, created_at) " +
        "VALUES " +
        "  ('group_cell_embu', 'Embu Cell Fellowship', 'Weekly home fellowship, prayer, and mutual care for Embu saints.', $1, TRUE, $2), " +
        "  ('group_worship_team', 'Praise & Worship Team', 'Choir rehearsals, song lineups, and sanctuary service coordination.', $1, TRUE, $2) " +
        "ON CONFLICT (id) DO NOTHING",
        [creatorId, now]
      );
      console.log("✅ Seeded default owner-managed groups");
    }

    const today = getNairobiDateString();
    const chalRes = await query("SELECT COUNT(*) FROM daily_challenges WHERE date = $1", [today]);
    if (parseInt(chalRes.rows[0].count, 10) === 0) {
      const defaultRiddles = [
        {
          id: "rid_01",
          q: "I had no father, no mother, and no birthday, yet I was created full grown. Who was I?",
          a: "Adam",
          hint: "Genesis 2:7"
        },
        {
          id: "rid_02",
          q: "I was a prophet swallowed by a great fish when running from God. Who am I?",
          a: "Jonah",
          hint: "Book of Jonah 1:17"
        },
        {
          id: "rid_03",
          q: "I defeated a giant warrior with only a sling and five smooth stones. Who am I?",
          a: "David",
          hint: "1 Samuel 17"
        },
        {
          id: "rid_04",
          q: "I was sold by my brothers for twenty pieces of silver, but God made me second in Egypt. Who am I?",
          a: "Joseph",
          hint: "Genesis 37:28"
        }
      ];
      const dayNum = parseInt(today.replace(/-/g, ""), 10) || 1;
      const selected = defaultRiddles[dayNum % defaultRiddles.length];
      await query(
        "INSERT INTO daily_challenges (date, riddle_id, question, answer, hint, created_at) " +
        "VALUES ($1, $2, $3, $4, $5, $6) " +
        "ON CONFLICT (date) DO NOTHING",
        [today, selected.id, selected.q, selected.a, selected.hint, Date.now()]
      );
      console.log("✅ Seeded daily Bible challenge for " + today);
    }
  } catch (err) {
    console.error("Seed data error:", err);
  }
}

setTimeout(ensureSeedData, 1000);

// ============================================================================
// 0. MEMBER CONNECTIONS (server-authoritative; QR payloads contain an account ID)
// ============================================================================

router.get('/connections', requireAuth, async (req, res) => {
  try {
    const result = await query(
      `SELECT c.id,c.requester_id,c.recipient_id,c.status,c.created_at,
              peer.id AS peer_id,peer.name AS peer_name
         FROM connections c
         JOIN users peer ON peer.id=CASE WHEN c.requester_id=$1 THEN c.recipient_id ELSE c.requester_id END
        WHERE (c.requester_id=$1 OR c.recipient_id=$1)
          AND c.status='ACCEPTED' AND peer.is_active=TRUE
        ORDER BY c.created_at DESC`, [req.user.id]
    );
    return res.json({ connections: result.rows });
  } catch (error) {
    console.error('Error fetching member connections:', error);
    return res.status(500).json({ error: 'Failed to fetch member connections' });
  }
});

router.get('/connections/pending', requireAuth, async (req, res) => {
  try {
    const result = await query(
      `SELECT c.id,c.requester_id,c.recipient_id,c.status,c.created_at,
              requester.id AS peer_id,requester.name AS peer_name
         FROM connections c JOIN users requester ON requester.id=c.requester_id
        WHERE c.recipient_id=$1 AND c.status='PENDING' AND requester.is_active=TRUE
        ORDER BY c.created_at DESC`, [req.user.id]
    );
    return res.json({ requests: result.rows });
  } catch (error) {
    console.error('Error fetching pending member connections:', error);
    return res.status(500).json({ error: 'Failed to fetch pending connections' });
  }
});

router.post('/connections', requireAuth, async (req, res) => {
  const peerId = typeof req.body?.peer_id === 'string' ? req.body.peer_id.trim() : '';
  if (!peerId || peerId.length > 64) return res.status(400).json({ error: 'A valid member account ID is required' });
  if (peerId === req.user.id) return res.status(400).json({ error: 'You cannot connect with yourself' });

  try {
    const outcome = await withTransaction(async client => {
      const ids = [req.user.id, peerId].sort();
      // Serialize both directions of a pair across Node processes and hosts sharing PostgreSQL.
      await client.query('SELECT pg_advisory_xact_lock(hashtext($1),hashtext($2))', ids);
      const people = await client.query(
        'SELECT id,name FROM users WHERE id=ANY($1::text[]) AND is_active=TRUE ORDER BY id FOR SHARE',
        [[req.user.id, peerId]]
      );
      const actor = people.rows.find(row => row.id === req.user.id);
      const peer = people.rows.find(row => row.id === peerId);
      if (!actor) return { status: 401, body: { error: 'Account is unavailable' } };
      if (!peer) return { status: 404, body: { error: 'Member account not found' } };

      const prior = await client.query(
        `SELECT id,requester_id,recipient_id,status,created_at FROM connections
          WHERE (requester_id=$1 AND recipient_id=$2) OR (requester_id=$2 AND recipient_id=$1)
          ORDER BY created_at DESC FOR UPDATE`, [req.user.id, peerId]
      );
      if (prior.rows.some(row => row.status === 'BLOCKED')) {
        return { status: 403, body: { error: 'This connection is unavailable' } };
      }
      if (prior.rows.some(row => row.status === 'ACCEPTED')) {
        return { status: 409, body: { error: 'These members are already connected' } };
      }
      const pending = prior.rows.find(row => row.status === 'PENDING');
      if (pending && pending.requester_id === req.user.id) {
        return { status: 200, body: { connection: { ...pending, peer_id: peerId, peer_name: peer.name } } };
      }
      if (pending) {
        return { status: 409, body: { error: 'An incoming request is already waiting for your response' } };
      }

      const now = Date.now();
      const existing = prior.rows[0];
      const saved = existing
        ? await client.query(
            `UPDATE connections SET requester_id=$2,recipient_id=$3,status='PENDING',created_at=$4
              WHERE id=$1 RETURNING id,requester_id,recipient_id,status,created_at`,
            [existing.id, req.user.id, peerId, now]
          )
        : await client.query(
            `INSERT INTO connections (id,requester_id,recipient_id,status,created_at)
             VALUES ($1,$2,$3,'PENDING',$4)
             RETURNING id,requester_id,recipient_id,status,created_at`,
            [crypto.randomUUID(), req.user.id, peerId, now]
          );
      const connection = { ...saved.rows[0], peer_id: peerId, peer_name: peer.name };
      await client.query(
        `INSERT INTO in_app_notifications
          (id,user_id,actor_id,actor_name,type,target_id,title,body,is_read,created_at)
         VALUES ($1,$2,$3,$4,'CONNECTION',$5,'Connection request', $6,FALSE,$7)`,
        [crypto.randomUUID(), peerId, req.user.id, actor.name, connection.id,
          `${actor.name} sent you a connection request.`, now]
      );
      return { status: 201, body: { connection } };
    });
    return res.status(outcome.status).json(outcome.body);
  } catch (error) {
    console.error('Error creating member connection:', error);
    return res.status(500).json({ error: 'Failed to create connection request' });
  }
});

router.post('/connections/:id/respond', requireAuth, async (req, res) => {
  const decision = typeof req.body?.decision === 'string' ? req.body.decision.toUpperCase() : '';
  if (!['ACCEPT', 'REJECT'].includes(decision)) return res.status(400).json({ error: 'Decision must be ACCEPT or REJECT' });
  try {
    const outcome = await withTransaction(async client => {
      const selected = await client.query(
        `SELECT c.id,c.requester_id,c.recipient_id,c.status,c.created_at,
                requester.name AS requester_name,recipient.name AS recipient_name
           FROM connections c
           JOIN users requester ON requester.id=c.requester_id
           JOIN users recipient ON recipient.id=c.recipient_id
          WHERE c.id=$1 AND c.recipient_id=$2 FOR UPDATE OF c`, [req.params.id, req.user.id]
      );
      if (!selected.rows.length) return { status: 404, body: { error: 'Pending connection request not found' } };
      const request = selected.rows[0];
      if (request.status !== 'PENDING') return { status: 409, body: { error: 'Connection request is no longer pending' } };
      const now = Date.now();
      const status = decision === 'ACCEPT' ? 'ACCEPTED' : 'REMOVED';
      const updated = await client.query(
        `UPDATE connections SET status=$2 WHERE id=$1
         RETURNING id,requester_id,recipient_id,status,created_at`, [request.id, status]
      );
      const title = decision === 'ACCEPT' ? 'Connection accepted' : 'Connection request declined';
      const body = decision === 'ACCEPT'
        ? `${request.recipient_name} accepted your connection request.`
        : `${request.recipient_name} declined your connection request.`;
      await client.query(
        `INSERT INTO in_app_notifications
          (id,user_id,actor_id,actor_name,type,target_id,title,body,is_read,created_at)
         VALUES ($1,$2,$3,$4,'CONNECTION',$5,$6,$7,FALSE,$8)`,
        [crypto.randomUUID(), request.requester_id, req.user.id, request.recipient_name,
          request.id, title, body, now]
      );
      return { status: 200, body: { connection: {
        ...updated.rows[0], peer_id: request.requester_id, peer_name: request.requester_name
      } } };
    });
    return res.status(outcome.status).json(outcome.body);
  } catch (error) {
    console.error('Error responding to member connection:', error);
    return res.status(500).json({ error: 'Failed to respond to connection request' });
  }
});

// ============================================================================
// 1. COMMUNITY FEED & POSTS
// ============================================================================

// GET /api/community/posts
router.get('/posts', optionalAuth, async (req, res) => {
  try {
    const { type, groupId, cursor, limit = 20 } = req.query;
    const currentUserId = req.user ? req.user.id : null;
    const parsedLimit = Math.min(Math.max(parseInt(limit, 10) || 20, 1), 50);

    const conditions = [];
    const params = [];
    let paramIndex = 1;

    if (type && type !== 'ALL') {
      conditions.push(`p.type = $${paramIndex++}`);
      params.push(type.toUpperCase());
    }

    if (groupId && !await canAccessGroup(req.user, groupId)) {
      return res.status(403).json({ error: 'This group is private. Ask its owner to add you.' });
    }
    if (groupId) {
      conditions.push(`p.group_id = $${paramIndex++}`);
      params.push(groupId);
    } else {
      conditions.push(`(p.group_id IS NULL OR p.group_id = '')`);
    }

    if (cursor) {
      conditions.push(`p.created_at < $${paramIndex++}`);
      params.push(parseInt(cursor, 10));
    }

    let userParamIndices = [];
    if (currentUserId) {
      userParamIndices.push(paramIndex++);
      params.push(currentUserId);
      userParamIndices.push(paramIndex++);
      params.push(currentUserId);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
    params.push(parsedLimit + 1);
    const limitParamIndex = paramIndex++;

    const queryStr = `
      SELECT
        p.id, p.author_id, p.author_name, p.type, p.title, p.content, p.media_url,
        p.group_id, p.is_pinned, p.is_answered, p.event_date, p.event_location,
        p.amen_count, p.comment_count, p.created_at, p.updated_at,
        u.avatar_url AS author_avatar,
        ${currentUserId ? `(EXISTS (SELECT 1 FROM post_amens pa WHERE pa.post_id = p.id AND pa.user_id = $${userParamIndices[0]}))` : 'FALSE'} AS user_has_amened,
        ${currentUserId ? `(SELECT er.status FROM event_rsvps er WHERE er.post_id = p.id AND er.user_id = $${userParamIndices[1]})` : 'NULL'} AS user_rsvp_status
      FROM community_posts p
      LEFT JOIN users u ON p.author_id = u.id
      ${whereClause}
      ORDER BY p.is_pinned DESC, p.created_at DESC
      LIMIT $${limitParamIndex}
    `;

    const result = await query(queryStr, params);
    const hasMore = result.rows.length > parsedLimit;
    const posts = hasMore ? result.rows.slice(0, parsedLimit) : result.rows;
    const nextCursor = posts.length > 0 ? posts[posts.length - 1].created_at : null;

    res.json({
      posts,
      hasMore,
      nextCursor
    });
  } catch (err) {
    console.error('Error fetching community posts:', err);
    res.status(500).json({ error: 'Failed to fetch community posts' });
  }
});

// POST /api/community/posts
router.post('/posts', requireAuth, async (req, res) => {
  try {
    const { id, type = 'GENERAL', title, content, mediaUrl = '', groupId = null, eventDate = null, eventLocation = '' } = req.body;

    if (!title || !title.trim()) {
      return res.status(400).json({ error: 'Post title is required' });
    }
    if (!content || !content.trim()) {
      return res.status(400).json({ error: 'Post content is required' });
    }

    if (!await ownsMedia(req.user.id, mediaUrl)) return res.status(400).json({ error: 'Choose a photo uploaded by your account' });
    if (String(type).toUpperCase() === 'EVENT' && (!Number.isFinite(Number(eventDate)) || Number(eventDate) <= 0)) return res.status(400).json({ error: 'Set a valid event date' });
    const allowedTypes = ['GENERAL', 'PRAYER', 'PRAISE', 'EVENT'];
    const postType = type.toUpperCase();
    if (!allowedTypes.includes(postType)) {
      return res.status(400).json({ error: `Invalid post type. Allowed: ${allowedTypes.join(', ')}` });
    }

    if (groupId) {
      const memberCheck = await query('SELECT 1 FROM group_memberships WHERE group_id = $1 AND user_id = $2', [groupId, req.user.id]);
      if (memberCheck.rows.length === 0 && (req.user.role !== 'ADMIN')) {
        return res.status(403).json({ error: 'You must be a member of this group to post.' });
      }
    }

    const postId = id || `post_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
    const now = Date.now();

    const insertQuery = `
      INSERT INTO community_posts (
        id, author_id, author_name, type, title, content, media_url,
        group_id, is_pinned, is_answered, event_date, event_location,
        amen_count, comment_count, created_at, updated_at
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, FALSE, FALSE, $9, $10, 0, 0, $11, $11)
      ON CONFLICT (id) DO UPDATE SET id = community_posts.id
        WHERE community_posts.author_id = EXCLUDED.author_id
      RETURNING *
    `;

    const result = await query(insertQuery, [
      postId,
      req.user.id,
      req.user.name || 'Fellow Saint',
      postType,
      title.trim(),
      content.trim(),
      mediaUrl,
      groupId,
      eventDate ? parseInt(eventDate, 10) : null,
      eventLocation ? eventLocation.trim() : '',
      now
    ]);

    if (!result.rows.length) return res.status(409).json({ error: 'Post identifier already in use' });

    if (postType === 'PRAYER' && !groupId) {
      await query(
        `INSERT INTO prayers (id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, FALSE, 'PUBLIC', 0, FALSE, '', $6, $6)
         ON CONFLICT (id) DO NOTHING`,
        [postId, title.trim(), content.trim(), req.user.id, req.user.name || 'Fellow Saint', now]
      );
    }

    res.status(201).json({
      post: result.rows[0]
    });
  } catch (err) {
    console.error('Error creating post:', err);
    res.status(500).json({ error: 'Failed to create post' });
  }
});

// PUT /api/community/posts/:id
router.put('/posts/:id', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    const { title, content, isAnswered } = req.body;

    const postRes = await query('SELECT * FROM community_posts WHERE id = $1', [id]);
    if (postRes.rows.length === 0) {
      return res.status(404).json({ error: 'Post not found' });
    }

    const post = postRes.rows[0];
    if (post.author_id !== req.user.id && (req.user.role !== 'ADMIN')) {
      return res.status(403).json({ error: 'Unauthorized to edit this post' });
    }

    const updatedTitle = title !== undefined ? title.trim() : post.title;
    const updatedContent = content !== undefined ? content.trim() : post.content;
    const updatedAnswered = isAnswered !== undefined ? Boolean(isAnswered) : post.is_answered;
    const now = Date.now();

    const updateRes = await query(
      `UPDATE community_posts
       SET title = $1, content = $2, is_answered = $3, updated_at = $4
       WHERE id = $5
       RETURNING *`,
      [updatedTitle, updatedContent, updatedAnswered, now, id]
    );

    res.json({ post: updateRes.rows[0] });
  } catch (err) {
    console.error('Error editing post:', err);
    res.status(500).json({ error: 'Failed to edit post' });
  }
});

// DELETE /api/community/posts/:id
router.delete('/posts/:id', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    const postRes = await query('SELECT * FROM community_posts WHERE id = $1', [id]);
    if (postRes.rows.length === 0) {
      return res.status(404).json({ error: 'Post not found' });
    }

    const post = postRes.rows[0];
    if (post.author_id !== req.user.id && (req.user.role !== 'ADMIN')) {
      return res.status(403).json({ error: 'Unauthorized to delete this post' });
    }

    await query('DELETE FROM community_posts WHERE id = $1', [id]);
    await query('DELETE FROM prayers WHERE id = $1', [id]);

    res.json({ success: true, message: 'Post deleted successfully' });
  } catch (err) {
    console.error('Error deleting post:', err);
    res.status(500).json({ error: 'Failed to delete post' });
  }
});

// POST /api/community/posts/:id/amen
router.post('/posts/:id/amen', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessPost(req.user, id)) return res.status(404).json({ error: 'Post unavailable' });
    const userId = req.user.id;
    const now = Date.now();

    const postCheck = await query('SELECT id, amen_count FROM community_posts WHERE id = $1', [id]);
    if (postCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Post not found' });
    }

    const outcome = await withTransaction(async (client) => {
      const existing = await client.query('SELECT 1 FROM post_amens WHERE post_id = $1 AND user_id = $2', [id, userId]);
      if (existing.rows.length > 0) {
        return { amenCount: postCheck.rows[0].amen_count, hasAmened: true };
      }
      const added = await client.query('INSERT INTO post_amens (post_id, user_id, created_at) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING RETURNING post_id', [id, userId, now]);
      if (!added.rows.length) return { amenCount: (await client.query('SELECT amen_count FROM community_posts WHERE id = $1', [id])).rows[0].amen_count, hasAmened: true };
      const updateRes = await client.query(
        'UPDATE community_posts SET amen_count = amen_count + 1 WHERE id = $1 RETURNING amen_count',
        [id]
      );
      return { amenCount: updateRes.rows[0].amen_count, hasAmened: true };
    });

    res.json({
      success: true,
      amenCount: outcome.amenCount,
      hasAmened: outcome.hasAmened
    });
  } catch (err) {
    console.error('Error recording amen:', err);
    res.status(500).json({ error: 'Failed to record Amen' });
  }
});

// GET /api/community/posts/:id/comments
router.get('/posts/:id/comments', optionalAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessPost(req.user, id)) return res.status(404).json({ error: 'Post unavailable' });
    const result = await query(
      `SELECT c.id, c.post_id, c.author_id, c.author_name, c.content, c.created_at,
              u.avatar_url AS author_avatar
       FROM post_comments c
       LEFT JOIN users u ON c.author_id = u.id
       WHERE c.post_id = $1
       ORDER BY c.created_at ASC`,
      [id]
    );

    res.json({ comments: result.rows });
  } catch (err) {
    console.error('Error fetching comments:', err);
    res.status(500).json({ error: 'Failed to fetch comments' });
  }
});

// POST /api/community/posts/:id/comments
router.post('/posts/:id/comments', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessPost(req.user, id)) return res.status(404).json({ error: 'Post unavailable' });
    const { content, id: suppliedId } = req.body;

    if (!content || !content.trim()) {
      return res.status(400).json({ error: 'Comment content cannot be empty' });
    }

    const postCheck = await query('SELECT author_id, title FROM community_posts WHERE id = $1', [id]);
    if (postCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Post not found' });
    }

    const post = postCheck.rows[0];
    const commentId = suppliedId || `comm_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
    const previous = await query('SELECT * FROM post_comments WHERE id = $1', [commentId]);
    if (previous.rows.length) {
      if (previous.rows[0].author_id !== req.user.id || previous.rows[0].post_id !== id)
        return res.status(409).json({ error: 'Comment identifier already in use' });
      return res.json({ comment: previous.rows[0] });
    }
    const now = Date.now();

    const comment = await withTransaction(async (client) => {
      const commentRes = await client.query(
        `INSERT INTO post_comments (id, post_id, author_id, author_name, content, created_at)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [commentId, id, req.user.id, req.user.name || 'Fellow Saint', content.trim(), now]
      );

      await client.query('UPDATE community_posts SET comment_count = comment_count + 1 WHERE id = $1', [id]);

      if (post.author_id !== req.user.id) {
        const notifId = `notif_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
        await client.query(
          `INSERT INTO in_app_notifications (id, user_id, actor_id, actor_name, type, target_id, title, body, is_read, created_at)
           VALUES ($1, $2, $3, $4, 'COMMENT', $5, $6, $7, FALSE, $8)`,
          [
            notifId,
            post.author_id,
            req.user.id,
            req.user.name || 'A saint',
            id,
            'New Comment on your post',
            `${req.user.name || 'A member'} commented: "${content.trim().slice(0, 80)}"`,
            now
          ]
        );
      }
      return commentRes.rows[0];
    });

    res.status(201).json({ comment });
  } catch (err) {
    console.error('Error adding comment:', err);
    res.status(500).json({ error: 'Failed to add comment' });
  }
});

// ============================================================================
// 2. OWNER-MANAGED GROUPS
// ============================================================================

// GET /api/community/groups
router.get('/groups', requireAuth, async (req, res) => {
  await ensureSeedData();
  try {
    const result = await query(
      `SELECT g.id, g.name, g.description, g.created_by, g.is_active, g.created_at,
              (SELECT COUNT(*) FROM group_memberships gm WHERE gm.group_id = g.id) AS member_count,
              (EXISTS (SELECT 1 FROM group_memberships gm WHERE gm.group_id = g.id AND gm.user_id = $1)) AS is_member
       FROM community_groups g
       WHERE g.is_active = TRUE AND ($2 = 'ADMIN' OR g.created_by = $1 OR EXISTS
         (SELECT 1 FROM group_memberships x WHERE x.group_id = g.id AND x.user_id = $1))
       ORDER BY g.created_at ASC`,
      [req.user.id, req.user.role]
    );

    res.json({ groups: result.rows });
  } catch (err) {
    console.error('Error fetching groups:', err);
    res.status(500).json({ error: 'Failed to fetch groups' });
  }
});

// GET /api/community/groups/:id/members
router.get('/groups/:id/members', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessGroup(req.user, id)) return res.status(403).json({ error: 'This group is private' });
    const result = await query(
      `SELECT gm.group_id, gm.user_id, gm.role, gm.joined_at,
              u.name, u.avatar_url
       FROM group_memberships gm
       JOIN users u ON gm.user_id = u.id
       WHERE gm.group_id = $1
       ORDER BY gm.joined_at ASC`,
      [id]
    );

    res.json({ members: result.rows });
  } catch (err) {
    console.error('Error fetching group members:', err);
    res.status(500).json({ error: 'Failed to fetch group members' });
  }
});

// POST /api/community/groups (Admin only)
router.post('/groups', requireAuth, async (req, res) => {
  try {
    const { id, name, description = '' } = req.body;
    if (!name || !name.trim()) {
      return res.status(400).json({ error: 'Group name is required' });
    }

    const groupId = id || `group_${Date.now()}_${crypto.randomBytes(3).toString('hex')}`;
    const now = Date.now();

    const group = await withTransaction(async (client) => {
      const groupRes = await client.query(
        `INSERT INTO community_groups (id, name, description, created_by, is_active, created_at)
         VALUES ($1, $2, $3, $4, TRUE, $5)
         RETURNING *`,
        [groupId, name.trim(), description.trim(), req.user.id, now]
      );

      await client.query(
        `INSERT INTO group_memberships (group_id, user_id, role, joined_at)
         VALUES ($1, $2, 'LEADER', $3)`,
        [groupId, req.user.id, now]
      );
      return groupRes.rows[0];
    });

    res.status(201).json({ group });
  } catch (err) {
    console.error('Error creating group:', err);
    res.status(500).json({ error: 'Failed to create group' });
  }
});

// POST /api/community/groups/:id/members (Admin only)
router.post('/groups/:id/members', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canManageGroup(req.user, id)) return res.status(403).json({ error: 'Only this group owner or an administrator can manage members' });
    const { userId } = req.body;
    const role = 'MEMBER';

    if (!userId) {
      return res.status(400).json({ error: 'userId is required' });
    }

    const now = Date.now();
    await query(
      `INSERT INTO group_memberships (group_id, user_id, role, joined_at)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (group_id, user_id) DO UPDATE SET role = EXCLUDED.role`,
      [id, userId, role, now]
    );

    res.json({ success: true, message: 'Member added to group successfully' });
  } catch (err) {
    console.error('Error adding member to group:', err);
    res.status(500).json({ error: 'Failed to add member to group' });
  }
});

// ============================================================================
// 3. PRIVATE 1-TO-1 MESSAGING
// ============================================================================

function getConversationId(u1, u2) {
  return [u1, u2].sort().join('_');
}

// GET /api/community/conversations
router.get('/conversations', requireAuth, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await query(
      `SELECT DISTINCT ON (
         CASE WHEN m.sender_id = $1 THEN m.receiver_id ELSE m.sender_id END
       )
         CASE WHEN m.sender_id = $1 THEN m.receiver_id ELSE m.sender_id END AS partner_id,
         u.name AS partner_name,
         u.avatar_url AS partner_avatar,
         m.id AS last_message_id,
         m.content AS last_message_content,
         m.media_url AS last_message_media,
         m.delivery_status AS last_delivery_status,
         m.created_at AS last_message_time,
         (
           SELECT COUNT(*) FROM messages unread
           WHERE unread.sender_id = (CASE WHEN m.sender_id = $1 THEN m.receiver_id ELSE m.sender_id END)
             AND unread.receiver_id = $1
             AND unread.delivery_status != 'READ'
         ) AS unread_count
       FROM messages m
       JOIN users u ON u.id = (CASE WHEN m.sender_id = $1 THEN m.receiver_id ELSE m.sender_id END)
       WHERE m.sender_id = $1 OR m.receiver_id = $1
       ORDER BY (CASE WHEN m.sender_id = $1 THEN m.receiver_id ELSE m.sender_id END), m.created_at DESC`,
      [userId]
    );

    res.json({ conversations: result.rows });
  } catch (err) {
    console.error('Error fetching conversations:', err);
    res.status(500).json({ error: 'Failed to fetch conversations' });
  }
});

// GET /api/community/messages/:partnerId
router.get('/messages/:partnerId', requireAuth, async (req, res) => {
  try {
    const { partnerId } = req.params;
    const userId = req.user.id;

    const result = await query(
      `SELECT m.id, m.conversation_id, m.sender_id, m.receiver_id, m.content,
              m.media_url, m.delivery_status, m.created_at, m.delivered_at,
              u.name AS sender_name
       FROM messages m
       JOIN users u ON m.sender_id = u.id
       WHERE (m.sender_id = $1 AND m.receiver_id = $2)
          OR (m.sender_id = $2 AND m.receiver_id = $1)
       ORDER BY m.created_at ASC
       LIMIT 100`,
      [userId, partnerId]
    );

    res.json({ messages: result.rows });
  } catch (err) {
    console.error('Error fetching messages:', err);
    res.status(500).json({ error: 'Failed to fetch messages' });
  }
});

// POST /api/community/messages
router.post('/messages', requireAuth, async (req, res) => {
  try {
    const { id, receiverId, content, mediaUrl = '' } = req.body;

    if (!receiverId) {
      return res.status(400).json({ error: 'receiverId is required' });
    }
    if ((!content || !content.trim()) && !mediaUrl) {
      return res.status(400).json({ error: 'Message content or media attachment is required' });
    }

    if (!await ownsMedia(req.user.id, mediaUrl)) return res.status(400).json({ error: 'Choose a photo uploaded by your account' });
    const receiverCheck = await query('SELECT name FROM users WHERE id = $1', [receiverId]);
    if (receiverCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Recipient member not found' });
    }

    if (receiverId === req.user.id) return res.status(400).json({ error: 'Choose another member' });
    const convId = getConversationId(req.user.id, receiverId);
    const msgId = id || `msg_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
    const previous = await query('SELECT * FROM messages WHERE id = $1', [msgId]);
    if (previous.rows.length) {
      if (previous.rows[0].sender_id !== req.user.id || previous.rows[0].receiver_id !== receiverId)
        return res.status(409).json({ error: 'Message identifier already in use' });
      return res.json({ message: previous.rows[0] });
    }
    const now = Date.now();

    const message = await withTransaction(async (client) => {
      const insertRes = await client.query(
        `INSERT INTO messages (id, conversation_id, sender_id, receiver_id, content, media_url, delivery_status, created_at, delivered_at)
         VALUES ($1, $2, $3, $4, $5, $6, 'SENT', $7, NULL)
         RETURNING *`,
        [msgId, convId, req.user.id, receiverId, (content || '').trim(), mediaUrl, now]
      );

      const notifId = `notif_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
      await client.query(
        `INSERT INTO in_app_notifications (id, user_id, actor_id, actor_name, type, target_id, title, body, is_read, created_at)
         VALUES ($1, $2, $3, $4, 'MESSAGE', $5, $6, $7, FALSE, $8)`,
        [
          notifId,
          receiverId,
          req.user.id,
          req.user.name || 'A saint',
          req.user.id,
          `New Message from ${req.user.name || 'A member'}`,
          content ? content.slice(0, 80) : '📷 Photo attachment',
          now
        ]
      );
      return insertRes.rows[0];
    });

    res.status(201).json({ message });
  } catch (err) {
    console.error('Error sending message:', err);
    res.status(500).json({ error: 'Failed to send message' });
  }
});

// POST /api/community/messages/ack
router.post('/messages/ack', requireAuth, async (req, res) => {
  try {
    const { messageIds = [], status = 'READ' } = req.body;
    if (!['DELIVERED', 'READ'].includes(status)) return res.status(400).json({ error: 'Invalid acknowledgement status' });
    if (!Array.isArray(messageIds) || messageIds.length === 0) {
      return res.json({ updated: 0 });
    }

    const resUpdate = await query(
      `UPDATE messages
       SET delivery_status = $1
       WHERE id = ANY($2::text[]) AND receiver_id = $3 AND delivery_status != 'READ'
       RETURNING id`,
      [status, messageIds, req.user.id]
    );

    res.json({ updated: resUpdate.rowCount });
  } catch (err) {
    console.error('Error acknowledging messages:', err);
    res.status(500).json({ error: 'Failed to acknowledge messages' });
  }
});

// ============================================================================
// 4. MEDIA ATTACHMENTS (IMAGE UPLOAD & VALIDATION)
// ============================================================================

// POST /api/community/media/upload
router.post('/media/upload', requireAuth, async (req, res) => {
  try {
    const { base64Data, filename } = req.body;

    if (!base64Data) {
      return res.status(400).json({ error: 'base64Data string is required' });
    }

    const cleanBase64 = base64Data.replace(/^data:image\/[a-zA-Z]+;base64,/, '');
    const buffer = Buffer.from(cleanBase64, 'base64');

    const validated = validateAndSanitizeImage(buffer);

    const ext = validated.format === 'jpeg' ? 'jpg' : validated.format;
    const saveFilename = `att_${Date.now()}_${crypto.randomBytes(4).toString('hex')}.${ext}`;
    const filePath = path.join(UPLOADS_DIR, saveFilename);

    fs.writeFileSync(filePath, validated.buffer);
    try {
      await query('INSERT INTO community_media (filename, owner_id, created_at) VALUES ($1, $2, $3)', [saveFilename, req.user.id, Date.now()]);
    } catch (error) { fs.unlinkSync(filePath); throw error; }

    res.status(201).json({
      mediaUrl: `/uploads/${saveFilename}`,
      format: validated.format,
      width: validated.width,
      height: validated.height,
      sizeBytes: validated.sizeBytes
    });
  } catch (err) {
    console.error('Media upload validation error:', err.message);
    res.status(400).json({ error: err.message });
  }
});

// ============================================================================
// 5. EVENTS & RSVPS
// ============================================================================

// POST /api/community/posts/:id/rsvp
router.post('/posts/:id/rsvp', requireAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessPost(req.user, id)) return res.status(404).json({ error: 'Post unavailable' });
    const { status = 'ATTENDING' } = req.body;
    const validStatuses = ['ATTENDING', 'DECLINED'];

    if (!validStatuses.includes(status.toUpperCase())) {
      return res.status(400).json({ error: 'Invalid status. Allowed: ATTENDING, DECLINED' });
    }

    const postCheck = await query("SELECT type FROM community_posts WHERE id = $1 AND type = 'EVENT'", [id]);
    if (postCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Event post not found' });
    }

    const rsvpStatus = status.toUpperCase();
    const now = Date.now();

    await query(
      `INSERT INTO event_rsvps (post_id, user_id, status, updated_at)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (post_id, user_id) DO UPDATE SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at`,
      [id, req.user.id, rsvpStatus, now]
    );

    const countRes = await query(
      `SELECT
         COUNT(*) FILTER (WHERE status = 'ATTENDING') AS attending_count,
         COUNT(*) FILTER (WHERE status = 'DECLINED') AS declined_count
       FROM event_rsvps
       WHERE post_id = $1`,
      [id]
    );

    res.json({
      success: true,
      status: rsvpStatus,
      attendingCount: parseInt(countRes.rows[0].attending_count, 10),
      declinedCount: parseInt(countRes.rows[0].declined_count, 10)
    });
  } catch (err) {
    console.error('Error recording RSVP:', err);
    res.status(500).json({ error: 'Failed to record RSVP' });
  }
});

// GET /api/community/posts/:id/rsvps
router.get('/posts/:id/rsvps', optionalAuth, async (req, res) => {
  try {
    const { id } = req.params;
    if (!await canAccessPost(req.user, id)) return res.status(404).json({ error: 'Post unavailable' });
    const countRes = await query(
      `SELECT
         COUNT(*) FILTER (WHERE status = 'ATTENDING') AS attending_count,
         COUNT(*) FILTER (WHERE status = 'DECLINED') AS declined_count
       FROM event_rsvps
       WHERE post_id = $1`,
      [id]
    );

    let userStatus = null;
    if (req.user) {
      const userRsvp = await query('SELECT status FROM event_rsvps WHERE post_id = $1 AND user_id = $2', [id, req.user.id]);
      if (userRsvp.rows.length > 0) {
        userStatus = userRsvp.rows[0].status;
      }
    }

    res.json({
      attendingCount: parseInt(countRes.rows[0].attending_count, 10),
      declinedCount: parseInt(countRes.rows[0].declined_count, 10),
      userStatus
    });
  } catch (err) {
    console.error('Error fetching RSVPs:', err);
    res.status(500).json({ error: 'Failed to fetch RSVPs' });
  }
});

// ============================================================================
// 6. DAILY BIBLE CHALLENGE
// ============================================================================

// GET /api/community/challenge/today
router.get('/challenge/today', optionalAuth, async (req, res) => {
  try {
    await ensureSeedData();
    const today = getNairobiDateString();

    const chalRes = await query('SELECT * FROM daily_challenges WHERE date = $1', [today]);
    if (chalRes.rows.length === 0) {
      return res.status(404).json({ error: 'No challenge scheduled for today' });
    }

    const challenge = chalRes.rows[0];
    let isCompleted = false;

    if (req.user) {
      const compCheck = await query(
        'SELECT 1 FROM user_challenge_completions WHERE user_id = $1 AND date = $2',
        [req.user.id, today]
      );
      isCompleted = compCheck.rows.length > 0;
    }

    res.json({
      date: challenge.date,
      riddleId: challenge.riddle_id,
      question: challenge.question,
      hint: challenge.hint,
      isCompleted
    });
  } catch (err) {
    console.error('Error fetching daily challenge:', err);
    res.status(500).json({ error: 'Failed to fetch daily challenge' });
  }
});

// POST /api/community/challenge/submit
router.post('/challenge/submit', requireAuth, async (req, res) => {
  try {
    const { answer } = req.body;
    if (!answer || !answer.trim()) {
      return res.status(400).json({ error: 'Answer is required' });
    }

    const today = getNairobiDateString();
    const chalRes = await query('SELECT * FROM daily_challenges WHERE date = $1', [today]);
    if (chalRes.rows.length === 0) {
      return res.status(404).json({ error: 'No challenge scheduled for today' });
    }

    const challenge = chalRes.rows[0];

    const compCheck = await query(
      'SELECT 1 FROM user_challenge_completions WHERE user_id = $1 AND date = $2',
      [req.user.id, today]
    );
    if (compCheck.rows.length > 0) {
      return res.json({
        success: true,
        alreadyCompleted: true,
        message: 'You have already completed today\'s challenge! Great job.',
        xpAwarded: 0
      });
    }

    const normalize = str => str.toLowerCase().replace(/[^a-z0-9]/g, '');
    const isCorrect = normalize(answer) === normalize(challenge.answer) ||
                      normalize(answer).includes(normalize(challenge.answer));

    if (!isCorrect) {
      return res.json({
        success: false,
        message: 'Not quite! Ponder the scripture hint and try again.',
        hint: challenge.hint
      });
    }

    const now = Date.now();
    await withTransaction(async (client) => {
      await client.query(
        'INSERT INTO user_challenge_completions (user_id, date, completed_at) VALUES ($1, $2, $3)',
        [req.user.id, today, now]
      );
      await client.query('UPDATE users SET xp = xp + 20 WHERE id = $1', [req.user.id]);
    });

    res.json({
      success: true,
      alreadyCompleted: false,
      message: 'Amen! Correct answer. +20 Spiritual XP awarded!',
      xpAwarded: 20
    });
  } catch (err) {
    console.error('Error submitting challenge:', err);
    res.status(500).json({ error: 'Failed to submit challenge' });
  }
});

// ============================================================================
// 7. IN-APP NOTIFICATIONS
// ============================================================================

// GET /api/community/notifications
router.get('/notifications', requireAuth, async (req, res) => {
  try {
    const result = await query(
      `SELECT id, user_id, actor_id, actor_name, type, target_id, title, body, is_read, created_at
       FROM in_app_notifications
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT 50`,
      [req.user.id]
    );

    const unreadCountRes = await query(
      'SELECT COUNT(*) FROM in_app_notifications WHERE user_id = $1 AND is_read = FALSE',
      [req.user.id]
    );

    res.json({
      notifications: result.rows,
      unreadCount: parseInt(unreadCountRes.rows[0].count, 10)
    });
  } catch (err) {
    console.error('Error fetching notifications:', err);
    res.status(500).json({ error: 'Failed to fetch notifications' });
  }
});

// POST /api/community/notifications/read
router.post('/notifications/read', requireAuth, async (req, res) => {
  try {
    const { notificationIds } = req.body;
    if (Array.isArray(notificationIds) && notificationIds.length > 0) {
      await query(
        'UPDATE in_app_notifications SET is_read = TRUE WHERE id = ANY($1::text[]) AND user_id = $2',
        [notificationIds, req.user.id]
      );
    } else {
      await query('UPDATE in_app_notifications SET is_read = TRUE WHERE user_id = $1', [req.user.id]);
    }

    res.json({ success: true });
  } catch (err) {
    console.error('Error marking notifications as read:', err);
    res.status(500).json({ error: 'Failed to update notifications' });
  }
});

// ============================================================================
// 8. MEMBER PROFILE (BIO & PHOTO UPDATE)
// ============================================================================

// PUT /api/community/profile
router.put('/profile', requireAuth, async (req, res) => {
  try {
    const { name, bio, avatarUrl } = req.body;

    const userRes = await query('SELECT * FROM users WHERE id = $1', [req.user.id]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const currentUser = userRes.rows[0];
    const updatedName = name !== undefined ? name.trim() : currentUser.name;
    const updatedBio = bio !== undefined ? bio.trim() : (currentUser.bio || '');
    if (avatarUrl !== undefined) {
      if (typeof avatarUrl !== 'string' || avatarUrl.length > 500) {
        return res.status(400).json({ error: 'Invalid profile picture reference' });
      }
      const candidateAvatar = avatarUrl.trim();
      const isDefaultAvatar = candidateAvatar === '' || /^default:(?:0?[1-9]|1[0-9]|20)$/.test(candidateAvatar);
      if (!isDefaultAvatar && !await ownsMedia(req.user.id, candidateAvatar)) {
        return res.status(400).json({ error: 'Profile picture must be a default avatar or an upload owned by this account' });
      }
    }
    const updatedAvatar = avatarUrl !== undefined ? avatarUrl.trim() : (currentUser.avatar_url || '');
    const now = Date.now();

    const updateRes = await query(
      `UPDATE users
       SET name = $1, bio = $2, avatar_url = $3, updated_at = $4
       WHERE id = $5
       RETURNING id, phone, name, role, campus_id, avatar_url, bio, xp, streak, grace_points`,
      [updatedName, updatedBio, updatedAvatar, now, req.user.id]
    );

    res.json({ user: updateRes.rows[0] });
  } catch (err) {
    console.error('Error updating profile:', err);
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

// Authenticated directory exposes names/IDs only, never phone numbers or emails.
router.get('/members', requireAuth, async (req, res) => {
  try {
    const search = String(req.query.search || '').trim().slice(0, 80);
    const result = await query(`SELECT id, name, avatar_url FROM users
      WHERE id != $1 AND name ILIKE $2 ORDER BY name, id LIMIT 50`, [req.user.id, `%${search}%`]);
    res.json({ members: result.rows });
  } catch (error) { res.status(500).json({ error: 'Could not load members' }); }
});

router.delete('/groups/:id/members/:userId', requireAuth, async (req, res) => {
  try {
    if (!await canManageGroup(req.user, req.params.id)) return res.status(403).json({ error: 'Only the group owner can remove members' });
    const owner = await query('SELECT created_by FROM community_groups WHERE id = $1', [req.params.id]);
    if (owner.rows[0].created_by === req.params.userId) return res.status(400).json({ error: 'The owner cannot be removed' });
    await query('DELETE FROM group_memberships WHERE group_id = $1 AND user_id = $2', [req.params.id, req.params.userId]);
    res.json({ success: true });
  } catch (error) { res.status(500).json({ error: 'Could not remove member' }); }
});

router.get('/classes', requireAuth, async (req, res) => {
  try {
    const result = await query(`SELECT c.*, g.name AS group_name, u.name AS teacher_name
      FROM community_classes c JOIN community_groups g ON g.id = c.group_id
      JOIN users u ON u.id = c.created_by
      WHERE g.is_active = TRUE AND ($2 = 'ADMIN' OR g.created_by = $1 OR EXISTS
        (SELECT 1 FROM group_memberships m WHERE m.group_id = g.id AND m.user_id = $1))
      ORDER BY c.starts_at DESC LIMIT 100`, [req.user.id, req.user.role]);
    res.json({ classes: result.rows });
  } catch (error) { res.status(500).json({ error: 'Could not load classes' }); }
});

router.post('/classes', requireAuth, async (req, res) => {
  try {
    const { groupId, title, description = '', startsAt, durationMinutes = 60 } = req.body;
    if (!groupId || !await canManageGroup(req.user, groupId)) return res.status(403).json({ error: 'Choose a group you own' });
    if (typeof title !== 'string' || !title.trim() || title.length > 255 ||
        !Number.isSafeInteger(Number(startsAt)) || Number(startsAt) < 1 ||
        !Number.isInteger(Number(durationMinutes)) || Number(durationMinutes) < 5 || Number(durationMinutes) > 480) {
      return res.status(400).json({ error: 'A title, valid date/time and duration of 5–480 minutes are required' });
    }
    const id = req.body.id || `class_${crypto.randomBytes(12).toString('hex')}`;
    const result = await query(`INSERT INTO community_classes
      (id, group_id, title, description, starts_at, duration_minutes, created_by, created_at)
      VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
      ON CONFLICT (id) DO UPDATE SET id = community_classes.id
        WHERE community_classes.created_by = EXCLUDED.created_by
      RETURNING *`, [id, groupId, title.trim(), String(description).slice(0, 10000),
      Number(startsAt), Number(durationMinutes), req.user.id, Date.now()]);
    if (!result.rows.length) return res.status(409).json({ error: 'Class identifier already in use' });
    res.status(201).json({ class: result.rows[0] });
  } catch (error) { res.status(500).json({ error: 'Could not schedule class' }); }
});

router.delete('/classes/:id', requireAuth, async (req, res) => {
  try {
    const result = await query(`UPDATE community_classes SET is_cancelled = TRUE
      WHERE id = $1 AND (created_by = $2 OR $3 = 'ADMIN') RETURNING id`,
      [req.params.id, req.user.id, req.user.role]);
    if (!result.rows.length) return res.status(403).json({ error: 'Only the teacher can cancel this class' });
    res.json({ success: true });
  } catch (error) { res.status(500).json({ error: 'Could not cancel class' }); }
});

module.exports = router;
module.exports.serveMedia = serveMedia;
