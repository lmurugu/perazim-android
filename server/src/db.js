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
  sync_events: [],
  revoked_tokens: [],
  privacy_notice_acknowledgements: [],
  privacy_presentation_receipts: [],
  data_subject_requests: [],
  data_subject_request_events: [],
  data_subject_request_revisions: []
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

  // INSERT INTO privacy_notice_acknowledgements
  if (lower.startsWith('insert into privacy_notice_acknowledgements')) {
    const [user_id, notice_version, content_hash, presented_at, acknowledged_at, acknowledgement_text] = params;
    memoryStore.privacy_notice_acknowledgements.push({ user_id, notice_version, content_hash, presented_at, acknowledged_at, acknowledgement_text, client_platform: 'android' });
    return { rows: [], rowCount: 1 };
  }

  // UPDATE users
  if (lower.startsWith('update users')) {
    const id = params[params.length - 1];
    const user = memoryStore.users.find(u => u.id === id || u.phone === id);
    if (user) {
      if (lower.includes('xp =') && params[0] !== undefined) user.xp = params[0];
      if (lower.includes('streak =') && params[1] !== undefined) user.streak = params[1];
      if (lower.includes('grace_points =') && params[0] !== undefined) user.grace_points = params[0];
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
    if (lower.includes('order by updated_at asc')) {
      let rows = [...memoryStore.prayers].sort((a, b) => {
        const aTime = a.updated_at !== undefined ? a.updated_at : a.created_at;
        const bTime = b.updated_at !== undefined ? b.updated_at : b.created_at;
        if (aTime !== bTime) {
          return aTime - bTime;
        }
        return (a.id || '').localeCompare(b.id || '');
      });

      if (lower.includes('updated_at = $1 and id > $2')) {
        const cursorTime = params[0];
        const cursorId = params[1];
        const limit = params[2];
        rows = rows.filter(p => {
          const pTime = p.updated_at !== undefined ? p.updated_at : p.created_at;
          return (pTime > cursorTime) || (pTime === cursorTime && p.id > cursorId);
        });
        if (typeof limit === 'number') {
          rows = rows.slice(0, limit);
        }
        return { rows };
      }

      if (lower.includes('updated_at > $1 or created_at > $1')) {
        const cursorTime = params[0];
        const limit = params[1];
        rows = rows.filter(p => {
          const pTime = p.updated_at !== undefined ? p.updated_at : p.created_at;
          return pTime > cursorTime || p.created_at > cursorTime;
        });
        if (typeof limit === 'number') {
          rows = rows.slice(0, limit);
        }
        return { rows };
      }

      if (lower.includes('updated_at >= $1')) {
        const minTime = params[0];
        const limit = params[1];
        rows = rows.filter(p => {
          const pTime = p.updated_at !== undefined ? p.updated_at : p.created_at;
          return pTime >= minTime;
        });
        if (typeof limit === 'number') {
          rows = rows.slice(0, limit);
        }
        return { rows };
      }

      const limit = params[params.length - 1];
      if (typeof limit === 'number') {
        rows = rows.slice(0, limit);
      }
      return { rows };
    }

    let rows = [...memoryStore.prayers].sort((a, b) => b.created_at - a.created_at);
    if (params.length > 0 && typeof params[0] === 'number') {
      rows = rows.filter(p => p.created_at > params[0] || (p.updated_at && p.updated_at > params[0]));
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
      return { rows: [prayer], rowCount: 1 };
    }
    return { rows: [existing], rowCount: 0 };
  }

  // UPDATE prayers SET prayer_count
  if (lower.startsWith('update prayers') && lower.includes('prayer_count = prayer_count + 1')) {
    const id = params[0];
    const prayer = memoryStore.prayers.find(p => p.id === id);
    if (prayer) {
      prayer.prayer_count = (prayer.prayer_count || 0) + 1;
      prayer.updated_at = Date.now();
    }
    return { rows: prayer ? [prayer] : [], rowCount: prayer ? 1 : 0 };
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
      return { rows: [], rowCount: 1 };
    }
    return { rows: [], rowCount: 0 };
  }

  // INSERT INTO sync_events
  if (lower.startsWith('insert into sync_events')) {
    const [id, user_id, entity_type, entity_id, operation_type, payload, created_at] = params;
    memoryStore.sync_events.push({ id, user_id, entity_type, entity_id, operation_type, payload, created_at });
    return { rows: [], rowCount: 1 };
  }

  // SELECT FROM revoked_tokens
  if (lower.startsWith('select') && lower.includes('from revoked_tokens')) {
    const hash = params[0];
    const item = memoryStore.revoked_tokens.find(t => t.token_hash === hash);
    return { rows: item ? [item] : [], rowCount: item ? 1 : 0 };
  }

  // INSERT INTO revoked_tokens
  if (lower.startsWith('insert into revoked_tokens')) {
    const [token_hash, user_id, expires_at, revoked_at] = params;
    const exists = memoryStore.revoked_tokens.some(t => t.token_hash === token_hash);
    if (!exists) {
      memoryStore.revoked_tokens.push({ token_hash, user_id, expires_at, revoked_at });
      return { rows: [], rowCount: 1 };
    }
    return { rows: [], rowCount: 0 };
  }

  return { rows: [], rowCount: 0 };
}

async function withTransaction(work) {
  if (isPostgres && pool) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const value = await work(client);
      await client.query('COMMIT');
      return value;
    } catch (error) {
      try { await client.query('ROLLBACK'); } catch (_) {}
      throw error;
    } finally { client.release(); }
  }
  const receipts = [...memoryStore.privacy_presentation_receipts];
  const acknowledgements = [...memoryStore.privacy_notice_acknowledgements];
  try {
    return await work({ query: async (sql, params = []) => {
      const normalized = sql.trim().toLowerCase();
      if (normalized.startsWith('insert into privacy_presentation_receipts')) {
        const [receipt_hash,user_id,notice_version,content_hash,presented_at,expires_at] = params;
        if (memoryStore.privacy_presentation_receipts.some(item => item.receipt_hash === receipt_hash)) throw new Error('duplicate receipt');
        memoryStore.privacy_presentation_receipts.push({receipt_hash,user_id,notice_version,content_hash,presented_at,expires_at,consumed_at:null});
        return {rows:[],rowCount:1};
      }
      if (normalized.startsWith('select') && normalized.includes('from privacy_presentation_receipts')) {
        const [receipt_hash,user_id,now] = params;
        const row = memoryStore.privacy_presentation_receipts.find(item => item.receipt_hash===receipt_hash && item.user_id===user_id && item.consumed_at===null && item.expires_at>now);
        return {rows:row?[{...row}]:[],rowCount:row?1:0};
      }
      if (normalized.startsWith('update privacy_presentation_receipts')) {
        const [consumed_at,receipt_hash,user_id] = params;
        const row=memoryStore.privacy_presentation_receipts.find(item=>item.receipt_hash===receipt_hash&&item.user_id===user_id&&item.consumed_at===null);
        if(row) row.consumed_at=consumed_at;
        return {rows:row?[{...row}]:[],rowCount:row?1:0};
      }
      if (normalized.startsWith('insert into privacy_notice_acknowledgements')) {
        const [user_id,notice_version,content_hash,presented_at,acknowledged_at,acknowledgement_text] = params;
        if(memoryStore.privacy_notice_acknowledgements.some(item=>item.user_id===user_id&&item.notice_version===notice_version)) throw new Error('duplicate acknowledgement');
        memoryStore.privacy_notice_acknowledgements.push({user_id,notice_version,content_hash,presented_at,acknowledged_at,acknowledgement_text,client_platform:'android'});
        return {rows:[],rowCount:1};
      }
      return query(sql, params);
    }});
  } catch (error) {
    memoryStore.privacy_presentation_receipts = receipts;
    memoryStore.privacy_notice_acknowledgements = acknowledgements;
    throw error;
  }
}

module.exports = {
  initDb,
  query,
  withTransaction,
  closeDb,
  isPostgresActive: () => isPostgres,
  memoryStore
};
