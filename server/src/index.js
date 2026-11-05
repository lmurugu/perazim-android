const express = require('express');
const cors = require('cors');
const { initDb } = require('./db');

const authRoutes = require('./routes/auth');
const prayerRoutes = require('./routes/prayers');
const campusRoutes = require('./routes/campuses');
const syncRoutes = require('./routes/sync');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Request logger
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl} ${res.statusCode} ${duration}ms`);
  });
  next();
});

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    church: 'Perazim Mission Church',
    version: '1.0.0',
    timestamp: Date.now()
  });
});

// Mount modular routes
app.use('/api/auth', authRoutes);
app.use('/api/prayers', prayerRoutes);
app.use('/api/campuses', campusRoutes);
app.use('/api/sync', syncRoutes);

// Global error handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Validate startup configuration
function validateStartup() {
  const isProd = process.env.NODE_ENV === 'production';
  const secret = process.env.JWT_SECRET;
  if (isProd) {
    if (!secret) {
      const err = new Error('FATAL: A secure JWT_SECRET environment variable is strictly required in production mode. Refusing to boot.');
      console.error(err.message);
      process.exit(1);
      throw err;
    }
    if (secret.length < 32 || secret === 'perazim-secret-key-2026') {
      const err = new Error('FATAL: JWT_SECRET in production must be at least 32 characters and cannot use default placeholders.');
      console.error(err.message);
      process.exit(1);
      throw err;
    }
  }
}

// Immediate evaluation in production
if (process.env.NODE_ENV === 'production') {
  validateStartup();
}

async function startServer() {
  validateStartup();
  try {
    await initDb();
  } catch (err) {
    console.error(`FATAL: Database connection failed: ${err.message}. Actionable: Ensure PostgreSQL is running and DATABASE_URL is valid, or explicitly set ALLOW_MEMORY_STORE=true to allow local in-memory fallback.`);
    process.exit(1);
    throw err;
  }
  return new Promise((resolve, reject) => {
    const server = app.listen(PORT, '0.0.0.0', () => {
      console.log(`🚀 Perazim Church Server listening on port ${PORT}`);
      resolve(server);
    });
    server.on('error', reject);
  });
}

if (require.main === module) {
  startServer();
}

module.exports = { app, startServer, validateStartup };
