const fs = require('fs');
const path = require('path');

let pool = null;
let isPostgres = false;

// In-memory fallback tables when PostgreSQL is not configured/available
const memoryStore = {
  campuses: [
    { id: 'campus_embu', name: 'Embu Headquarters', location: 'Blue Valley, Embu', contact_phone: '+254710772227', is_active: true, created_at: Date.now() },
    { id: 'campus_nairobi', name: 'Nairobi Fellowship', location: 'CBD, Nairobi', contact_phone: '+254710772227', is_active: true, created_at: Date.now() },
    { id: 'campus_meru', name: 'Meru Outreach', location: 'Town Center, Meru', contact_phone: '+254710772227', is_active: true, created_at: Date.now() },
    { id: 'campus_online', name: 'Online Global Campus', location: 'Worldwide Broadcast', contact_phone: '+254710772227', is_active: true, created_at: Date.now() }
  ],
  users: [],
  prayers: [],
  prayer_amens: [],
  messages: [],
  connections: [],
  sync_events: []
};

async function initDb() {
  const connectionString = process.env.DATABASE_URL;
  const isProd = process.env.NODE_ENV === 'production';
  const allowMemory = process.env.ALLOW_MEMORY_STORE === 'true';

  if (connectionString) {
    try {
      const { Pool } = require('pg');
      pool = new Pool({ connectionString });
      // Test connection
      await pool.query('SELECT 1');
      isPostgres = true;
      console.log('✅ Connected to PostgreSQL database');

      // Initialize schema
      const schemaSql = fs.readFileSync(path.join(__dirname, '../schema.sql'), 'utf8');
      await pool.query(schemaSql);
      console.log('✅ PostgreSQL schema verified & updated');
      return;
    } catch (err) {
      console.error('❌ FATAL: PostgreSQL connection failed with DATABASE_URL:', err.message);
      // NEVER silently fall back to memory store when DATABASE_URL was explicitly provided
      if (isProd || !allowMemory) {
        throw new Error(`Database connection failed: ${err.message}. Refusing to fall back to transient memory.`);
      }
      console.warn('⚠️ WARNING: Falling back to local memory store because ALLOW_MEMORY_STORE=true is set. DATA WILL BE LOST ON RESTART.');
      isPostgres = false;
    }
  } else {
    if (isProd) {
      throw new Error('FATAL: DATABASE_URL environment variable is required in production. Refusing to run on transient memory store.');
    }
    console.warn('⚠️ WARNING: No DATABASE_URL set. Running with transient in-memory store. ALL DATA WILL BE LOST ON RESTART.');
    isPostgres = false;
  }
}

async function closeDb() {
  if (pool) {
    await pool.end();
    pool = null;
    isPostgres = false;
  }
}

async function query(sql, params = []) {
  if (isPostgres && pool) {
    return pool.query(sql, params);
  }

  // Memory store query simulator for core CRUD operations
  const normalized = sql.trim();
  const lower = normalized.toLowerCase();

  // SELECT FROM campuses
  if (lower.startsWith('select') && lower.includes('from campuses')) {
    return { rows: [...memoryStore.campuses] };
  }

  // SELECT FROM users WHERE phone = $1
  if (lower.startsWith('select') && lower.includes('from users where phone = $1')) {
    const user = memoryStore.users.find(u => u.phone === params[0]);
    return { rows: user ? [user] : [] };
  }

  // SELECT FROM users WHERE id = $1
  if (lower.startsWith('select') && lower.includes('from users where id = $1')) {
    const user = memoryStore.users.find(u => u.id === params[0]);
    return { rows: user ? [user] : [] };
  }

  // INSERT INTO users
  if (lower.startsWith('insert into users')) {
    const [id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at] = params;
    const user = { id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at };
    memoryStore.users.push(user);
    return { rows: [user] };
  }

  // UPDATE users
  if (lower.startsWith('update users')) {
    const id = params[params.length - 1];
    const user = memoryStore.users.find(u => u.id === id);
    if (user) {
      if (lower.includes('xp =') && params[0] !== undefined) user.xp = params[0];
      if (lower.includes('streak =') && params[1] !== undefined) user.streak = params[1];
    }
    return { rows: user ? [user] : [] };
  }

  // SELECT FROM prayers WHERE id = $1
  if (lower.startsWith('select') && lower.includes('from prayers where id = $1')) {
    const prayer = memoryStore.prayers.find(p => p.id === params[0]);
    return { rows: prayer ? [prayer] : [] };
  }

  // SELECT FROM prayers
  if (lower.startsWith('select') && lower.includes('from prayers')) {
    let rows = [...memoryStore.prayers].sort((a, b) => b.created_at - a.created_at);
    if (params.length > 0 && typeof params[0] === 'number') {
      rows = rows.filter(p => p.created_at > params[0]);
    }
    return { rows };
  }

  // INSERT INTO prayers
  if (lower.startsWith('insert into prayers')) {
    const [id, title, content, author_id, author_name, is_anonymous, visibility, prayer_count, is_answered, testimony, created_at, updated_at] = params;
    const existing = memoryStore.prayers.find(p => p.id === id);
    if (!existing) {
      const prayer = { id, title, content, author_id, author_name, is_anonymous: !!is_anonymous, visibility: visibility || 'PUBLIC', prayer_count: prayer_count || 0, is_answered: !!is_answered, testimony: testimony || '', created_at, updated_at };
      memoryStore.prayers.push(prayer);
      return { rows: [prayer] };
    }
    return { rows: [existing] };
  }

  // UPDATE prayers SET prayer_count
  if (lower.startsWith('update prayers') && lower.includes('prayer_count = prayer_count + 1')) {
    const id = params[0];
    const prayer = memoryStore.prayers.find(p => p.id === id);
    if (prayer) {
      prayer.prayer_count = (prayer.prayer_count || 0) + 1;
      prayer.updated_at = Date.now();
    }
    return { rows: prayer ? [prayer] : [] };
  }

  // UPDATE prayers SET is_answered = true
  if (lower.startsWith('update prayers') && lower.includes('is_answered = true')) {
    const id = params[0];
    const testimony = params[1] || '';
    const updated_at = params[2] || Date.now();
    const prayer = memoryStore.prayers.find(p => p.id === id);
    if (prayer) {
      if (params.length > 3 && params[3] && prayer.author_id !== params[3]) {
        return { rows: [], rowCount: 0 };
      }
      prayer.is_answered = true;
      if (testimony) prayer.testimony = testimony;
      prayer.updated_at = updated_at;
      return { rows: [prayer], rowCount: 1 };
    }
    return { rows: [], rowCount: 0 };
  }

  // INSERT INTO prayer_amens
  if (lower.startsWith('insert into prayer_amens')) {
    const [prayer_id, user_id, created_at] = params;
    const exists = memoryStore.prayer_amens.some(a => a.prayer_id === prayer_id && a.user_id === user_id);
    if (!exists) {
      memoryStore.prayer_amens.push({ prayer_id, user_id, created_at });
    }
    return { rows: [] };
  }

  // INSERT INTO sync_events
  if (lower.startsWith('insert into sync_events')) {
    const [id, user_id, entity_type, entity_id, operation_type, payload, created_at] = params;
    memoryStore.sync_events.push({ id, user_id, entity_type, entity_id, operation_type, payload, created_at });
    return { rows: [] };
  }

  return { rows: [] };
}

module.exports = {
  initDb,
  query,
  closeDb,
  isPostgresActive: () => isPostgres,
  memoryStore
};
