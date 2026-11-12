const express = require('express');
const cors = require('cors');
const { initDb } = require('./db');
const { proxyTrustSetting } = require('./config/proxy');


const authRoutes = require('./routes/auth');
const prayerRoutes = require('./routes/prayers');
const campusRoutes = require('./routes/campuses');
const syncRoutes = require('./routes/sync');
const communityRoutes = require('./routes/community');
const membershipRoutes = require('./routes/membership');
const adminMembershipRoutes = require('./routes/adminMembership');
const privacyRoutes = require('./routes/privacy');
const aiRoutes = require('./routes/ai');

const app = express();
app.set('trust proxy', proxyTrustSetting(process.env.TRUST_PROXY_HOPS));
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use('/api/ai', aiRoutes);
app.use(express.json({ limit: '15mb' }));

// Attachment authorization is evaluated against its current post/chat scope.
app.get('/uploads/:filename', communityRoutes.serveMedia);

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
app.use('/api/privacy', privacyRoutes);
app.use('/api/prayers', prayerRoutes);
app.use('/api/campuses', campusRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/community', communityRoutes);
app.use('/api/classroom-calls', require('./routes/classroomCalls'));
app.use('/api', membershipRoutes);
app.use('/api/admin', adminMembershipRoutes);
app.use('/api/admin/ai-sources', require('./routes/knowledgeGovernance'));

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
    const bindAddress = process.env.NODE_ENV === 'test' ? '127.0.0.1' : '0.0.0.0';
    const server = app.listen(PORT, bindAddress, () => {
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
