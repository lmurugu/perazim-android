'use strict';

// Church-wide Bible games leaderboard.
// Owner decision (2026-11-18): the whole church, opt-in only, first name only.
// Points are stars earned from correct answers. The phone uploads them in small batches;
// each batch has a run id so a retried upload is never counted twice.

const express = require('express');
const { query, withTransaction, isPostgresActive } = require('../db');
const { requireAuth, optionalAuth } = require('../middleware/auth');

const router = express.Router();

const MAX_POINTS_PER_UPLOAD = 60;   // a long session of several games
const MAX_POINTS_PER_WEEK = 2000;   // keeps one runaway client from dominating the board
const LEADERBOARD_SIZE = 50;
const CHURCH_UTC_OFFSET_MS = 3 * 60 * 60 * 1000; // Kenya (EAT, UTC+3); weeks start on Monday.

// In-memory fallback for local development without PostgreSQL.
const memory = { profiles: new Map(), weeks: new Map(), events: new Set(), names: new Map() };

function weekStart(now = Date.now()) {
  const local = new Date(now + CHURCH_UTC_OFFSET_MS);
  const day = (local.getUTCDay() + 6) % 7; // Monday = 0
  local.setUTCDate(local.getUTCDate() - day);
  return local.toISOString().slice(0, 10);
}

function firstName(name) {
  const first = String(name || '').trim().split(/\s+/)[0] || 'Member';
  const clean = first.replace(/[^\p{L}\p{M}'-]/gu, '').slice(0, 20) || 'Member';
  return clean.charAt(0).toUpperCase() + clean.slice(1).toLowerCase();
}

function validUpload(body) {
  const { runId, game, points } = body || {};
  if (typeof runId !== 'string' || !/^[A-Za-z0-9_-]{8,64}$/.test(runId)) return 'A valid runId is required';
  if (typeof game !== 'string' || !/^[a-z_]{1,32}$/.test(game)) return 'A valid game is required';
  if (!Number.isInteger(points) || points < 0 || points > MAX_POINTS_PER_UPLOAD) return `points must be a whole number from 0 to ${MAX_POINTS_PER_UPLOAD}`;
  return null;
}

// GET /api/games/leaderboard/me
router.get('/leaderboard/me', requireAuth, async (req, res) => {
  try {
    if (!isPostgresActive()) return res.json({ optIn: !!memory.profiles.get(req.user.id), displayName: firstName(req.user.name) });
    const result = await query('SELECT opt_in FROM game_leaderboard_profiles WHERE user_id = $1', [req.user.id]);
    const row = result.rows && result.rows[0];
    return res.json({ optIn: !!(row && row.opt_in), displayName: firstName(req.user.name) });
  } catch (err) {
    console.error('Leaderboard profile read error');
    return res.status(500).json({ error: 'Server error loading leaderboard settings' });
  }
});

// PUT /api/games/leaderboard/me  { optIn: boolean }
router.put('/leaderboard/me', requireAuth, async (req, res) => {
  const optIn = req.body && req.body.optIn;
  if (typeof optIn !== 'boolean') return res.status(400).json({ error: 'optIn must be true or false' });
  try {
    if (!isPostgresActive()) {
      memory.profiles.set(req.user.id, optIn);
      memory.names.set(req.user.id, req.user.name);
    } else {
      await query(
        `INSERT INTO game_leaderboard_profiles (user_id, opt_in, updated_at) VALUES ($1, $2, $3)
         ON CONFLICT (user_id) DO UPDATE SET opt_in = EXCLUDED.opt_in, updated_at = EXCLUDED.updated_at`,
        [req.user.id, optIn, Date.now()]
      );
    }
    return res.json({ optIn, displayName: firstName(req.user.name) });
  } catch (err) {
    console.error('Leaderboard profile update error');
    return res.status(500).json({ error: 'Server error saving leaderboard settings' });
  }
});

// POST /api/games/scores  { runId, game, points }
router.post('/scores', requireAuth, async (req, res) => {
  const invalid = validUpload(req.body);
  if (invalid) return res.status(400).json({ error: invalid });
  const { runId, game, points } = req.body;
  const userId = req.user.id;
  const week = weekStart();
  const now = Date.now();
  try {
    if (!isPostgresActive()) {
      const eventKey = `${userId}:${runId}`;
      const weekKey = `${userId}:${week}`;
      memory.names.set(userId, req.user.name);
      if (memory.events.has(eventKey)) return res.json({ duplicate: true, weekPoints: memory.weeks.get(weekKey) || 0 });
      memory.events.add(eventKey);
      const total = Math.min(MAX_POINTS_PER_WEEK, (memory.weeks.get(weekKey) || 0) + points);
      memory.weeks.set(weekKey, total);
      return res.json({ duplicate: false, weekPoints: total });
    }
    const outcome = await withTransaction(async (client) => {
      const inserted = await client.query(
        `INSERT INTO game_score_events (user_id, run_id, game, points, created_at) VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (user_id, run_id) DO NOTHING RETURNING run_id`,
        [userId, runId, game, points, now]
      );
      if (!inserted.rows || inserted.rows.length === 0) {
        const current = await client.query('SELECT points FROM game_score_weeks WHERE user_id = $1 AND week_start = $2', [userId, week]);
        return { duplicate: true, weekPoints: current.rows[0] ? current.rows[0].points : 0 };
      }
      const updated = await client.query(
        `INSERT INTO game_score_weeks (user_id, week_start, points, updated_at) VALUES ($1, $2, LEAST($3::int, $5::int), $4)
         ON CONFLICT (user_id, week_start) DO UPDATE
           SET points = LEAST(game_score_weeks.points + $3::int, $5::int), updated_at = EXCLUDED.updated_at
         RETURNING points`,
        [userId, week, points, now, MAX_POINTS_PER_WEEK]
      );
      return { duplicate: false, weekPoints: updated.rows[0].points };
    });
    return res.json(outcome);
  } catch (err) {
    console.error('Game score upload error');
    return res.status(500).json({ error: 'Server error saving score' });
  }
});

// GET /api/games/leaderboard?period=week|all
// Only members who opted in are listed, by first name. A signed-in caller also gets their own rank.
router.get('/leaderboard', optionalAuth, async (req, res) => {
  const period = req.query.period === 'all' ? 'all' : 'week';
  const week = weekStart();
  const me = req.user ? req.user.id : null;
  try {
    let rows;
    if (!isPostgresActive()) {
      const totals = new Map();
      for (const [key, value] of memory.weeks) {
        const [userId, wk] = key.split(':');
        if (period === 'week' && wk !== week) continue;
        totals.set(userId, (totals.get(userId) || 0) + value);
      }
      rows = [...totals].filter(([id]) => memory.profiles.get(id))
        .map(([id, points]) => ({ user_id: id, name: memory.names.get(id), points }))
        .sort((a, b) => b.points - a.points);
    } else {
      const result = await query(
        `SELECT s.user_id, u.name, SUM(s.points)::int AS points, MIN(s.updated_at) AS first_at
           FROM game_score_weeks s
           JOIN game_leaderboard_profiles p ON p.user_id = s.user_id AND p.opt_in = TRUE
           JOIN users u ON u.id = s.user_id
          WHERE ($1::text = 'all' OR s.week_start = $2::date)
          GROUP BY s.user_id, u.name
         HAVING SUM(s.points) > 0
          ORDER BY points DESC, first_at ASC`,
        [period, week]
      );
      rows = result.rows || [];
    }
    let rank = 0; let lastPoints = null;
    const ranked = rows.filter(r => r.points > 0).map((r, i) => {
      if (r.points !== lastPoints) { rank = i + 1; lastPoints = r.points; }
      return { rank, name: firstName(r.name), points: r.points, me: r.user_id === me, userId: r.user_id };
    });
    const mine = me ? ranked.find(r => r.userId === me) : null;
    const entries = ranked.slice(0, LEADERBOARD_SIZE).map(({ userId, ...rest }) => rest);
    return res.json({
      period,
      weekStart: week,
      entries,
      me: mine ? { rank: mine.rank, points: mine.points } : null
    });
  } catch (err) {
    console.error('Leaderboard read error');
    return res.status(500).json({ error: 'Server error loading leaderboard' });
  }
});

module.exports = router;
module.exports._internal = { weekStart, firstName, validUpload, MAX_POINTS_PER_UPLOAD, MAX_POINTS_PER_WEEK };
