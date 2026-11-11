const express = require('express');
const { query } = require('../db');

const router = express.Router();

// GET /api/campuses
router.get('/', async (req, res) => {
  try {
    const result = await query('SELECT * FROM campuses WHERE is_active = TRUE ORDER BY name ASC');
    return res.json({ campuses: result.rows || [] });
  } catch (err) {
    console.error('Fetch campuses error');
    return res.status(500).json({ error: 'Server error loading campuses' });
  }
});

module.exports = router;
