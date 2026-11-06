const express = require('express');
const { query } = require('../db');
const { requireAuth } = require('../middleware/auth');
const { ClassroomCallStore, participantLimit } = require('../services/classroomCallStore');

async function assertClassAccess(queryFn, user, classId) {
  if (!/^[a-zA-Z0-9_-]{1,64}$/.test(classId || '')) {
    const error = new Error('Invalid class identifier'); error.status = 400; throw error;
  }
  const result = await queryFn(`SELECT c.id FROM community_classes c
    JOIN community_groups g ON g.id = c.group_id
    WHERE c.id = $1 AND c.is_cancelled = FALSE AND g.is_active = TRUE
      AND (c.created_by = $2 OR g.created_by = $2 OR $3 = 'ADMIN' OR EXISTS
        (SELECT 1 FROM group_memberships m WHERE m.group_id = g.id AND m.user_id = $2))`,
  [classId, user.id, user.role]);
  if (!result.rows.length) { const error = new Error('This class is unavailable or private'); error.status = 403; throw error; }
}

const store = new ClassroomCallStore({ maxParticipants: participantLimit(process.env.CLASSROOM_MAX_PARTICIPANTS) });
const router = express.Router();
router.use(requireAuth);
const wrap = action => async (req, res) => {
  try { await action(req, res); }
  catch (error) { res.status(error.status || 500).json({ error: error.status ? error.message : 'Classroom service unavailable' }); }
};
const access = req => assertClassAccess(query, req.user, req.params.classId);

router.post('/:classId/join', wrap(async (req, res) => {
  await access(req);
  res.json(store.join(req.params.classId, req.user, req.body.joinKey));
}));
router.get('/:classId/:sessionId/events', wrap(async (req, res) => {
  await access(req);
  const after = Number(req.query.after || 0);
  res.json(store.poll(req.params.classId, req.params.sessionId, req.user.id, after));
}));
router.post('/:classId/:sessionId/signal', wrap(async (req, res) => {
  await access(req);
  store.signal(req.params.classId, req.params.sessionId, req.user.id, req.body.target, req.body.payload);
  res.json({ accepted: true });
}));
router.delete('/:classId/:sessionId', wrap(async (req, res) => {
  // A removed member may still close their own session; they cannot poll or signal.
  store.leave(req.params.classId, req.params.sessionId, req.user.id);
  res.json({ left: true });
}));

module.exports = router;
module.exports.assertClassAccess = assertClassAccess;
