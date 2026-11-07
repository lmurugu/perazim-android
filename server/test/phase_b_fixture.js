const crypto = require('crypto');

function newId() {
  return crypto.randomUUID();
}

function quoteIdentifier(identifier) {
  if (!/^[a-z_][a-z0-9_]*$/i.test(identifier)) throw new Error('Fixture database contains an unsafe identifier.');
  return `"${identifier}"`;
}

async function assertPhaseBTestDatabase(pool) {
  const result = await pool.query('SELECT current_database() AS name');
  if (!/^perazim_phaseb_[a-z0-9_]+_test$/.test(result.rows[0].name)) {
    throw new Error('SAFETY ABORT: Phase B fixture writes require a uniquely named perazim_phaseb_*_test database.');
  }
}

async function createSyntheticExistingFixture(pool, suffix) {
  await assertPhaseBTestDatabase(pool);
  if (!/^[a-z0-9]{6,20}$/.test(suffix)) throw new Error('Fixture suffix must be a short random token.');
  const ids = {
    owner: `fixture_owner_${suffix}`,
    member: `fixture_member_${suffix}`,
    group: `fixture_group_${suffix}`,
    post: `fixture_post_${suffix}`,
    prayer: `fixture_prayer_${suffix}`,
    connection: `fixture_connection_${suffix}`
  };
  const now = Date.now();
  await pool.query(`
    INSERT INTO users (id, phone, pin_hash, name, campus_id, role, avatar_url, xp, streak, grace_points, is_active, created_at, updated_at, bio)
    VALUES ($1, $2, 'fixture-hash-owner', 'Synthetic Owner', 'campus_embu', 'MEMBER', '', 0, 0, 0, TRUE, $3, $3, 'synthetic fixture'),
           ($4, $5, 'fixture-hash-member', 'Synthetic Member', 'campus_nairobi', 'MEMBER', '', 2, 1, 9, TRUE, $3, $3, 'synthetic fixture')
  `, [ids.owner, `+254700${suffix.slice(0, 6)}`, now, ids.member, `+254701${suffix.slice(0, 6)}`]);
  await pool.query(`INSERT INTO prayers (id,title,content,author_id,author_name,is_anonymous,visibility,prayer_count,is_answered,testimony,created_at,updated_at) VALUES ($1,'Synthetic fixture prayer','Synthetic fixture content',$2,'Synthetic Owner',FALSE,'PUBLIC',1,FALSE,'',$3,$3)`, [ids.prayer, ids.owner, now]);
  await pool.query(`INSERT INTO prayer_amens (prayer_id,user_id,created_at) VALUES ($1,$2,$3)`, [ids.prayer, ids.member, now]);
  await pool.query(`INSERT INTO messages (id,conversation_id,sender_id,receiver_id,content,status,created_at,media_url,delivery_status,delivered_at) VALUES ($1,$2,$3,$4,'Synthetic fixture message','DELIVERED',$5,'','DELIVERED',NULL)`, [`fixture_message_${suffix}`, `fixture_conversation_${suffix}`, ids.owner, ids.member, now]);
  await pool.query(`INSERT INTO connections (id,requester_id,recipient_id,status,created_at) VALUES ($1,$2,$3,'ACCEPTED',$4)`, [ids.connection, ids.owner, ids.member, now]);
  await pool.query(`INSERT INTO sync_events (id,user_id,entity_type,entity_id,operation_type,payload,created_at) VALUES ($1,$2,'prayer',$3,'UPSERT','{"fixture":true}',$4)`, [`fixture_sync_${suffix}`, ids.owner, ids.prayer, now]);
  await pool.query(`INSERT INTO revoked_tokens (token_hash,user_id,expires_at,revoked_at) VALUES ($1,$2,$3,$4)`, [`fixture_revoked_${suffix}`, ids.member, now + 100000, now]);
  await pool.query(`INSERT INTO community_groups (id,name,description,created_by,is_active,created_at) VALUES ($1,'Synthetic Fixture Group','Synthetic fixture group',$2,TRUE,$3)`, [ids.group, ids.owner, now]);
  await pool.query(`INSERT INTO group_memberships (group_id,user_id,role,joined_at) VALUES ($1,$2,'MEMBER',$3)`, [ids.group, ids.member, now]);
  await pool.query(`INSERT INTO community_posts (id,author_id,author_name,type,title,content,media_url,group_id,is_pinned,is_answered,event_date,event_location,amen_count,comment_count,created_at,updated_at) VALUES ($1,$2,'Synthetic Owner','GENERAL','Synthetic fixture post','Synthetic fixture content','',$3,FALSE,FALSE,NULL,'',1,1,$4,$4)`, [ids.post, ids.owner, ids.group, now]);
  await pool.query(`INSERT INTO post_amens (post_id,user_id,created_at) VALUES ($1,$2,$3)`, [ids.post, ids.member, now]);
  await pool.query(`INSERT INTO post_comments (id,post_id,author_id,author_name,content,created_at) VALUES ($1,$2,$3,'Synthetic Member','Synthetic fixture comment',$4)`, [`fixture_comment_${suffix}`, ids.post, ids.member, now]);
  await pool.query(`INSERT INTO event_rsvps (post_id,user_id,status,updated_at) VALUES ($1,$2,'ATTENDING',$3)`, [ids.post, ids.member, now]);
  await pool.query(`INSERT INTO in_app_notifications (id,user_id,actor_id,actor_name,type,target_id,title,body,is_read,created_at) VALUES ($1,$2,$3,'Synthetic Owner','COMMENT',$4,'Synthetic fixture alert','Synthetic fixture body',FALSE,$5)`, [`fixture_notification_${suffix}`, ids.member, ids.owner, ids.post, now]);
  await pool.query(`INSERT INTO daily_challenges (date,riddle_id,question,answer,hint,created_at) VALUES ($1,'fixture_riddle','Synthetic fixture question','answer','hint',$2)`, [`2098-${suffix.slice(0, 2)}-${suffix.slice(2, 4)}`, now]);
  await pool.query(`INSERT INTO user_challenge_completions (user_id,date,completed_at) VALUES ($1,$2,$3)`, [ids.member, `2098-${suffix.slice(0, 2)}-${suffix.slice(2, 4)}`, now]);
  await pool.query(`INSERT INTO community_classes (id,group_id,title,description,starts_at,duration_minutes,meeting_url,created_by,is_cancelled,created_at) VALUES ($1,$2,'Synthetic fixture class','Synthetic fixture description',$3,60,'',$4,FALSE,$3)`, [`fixture_class_${suffix}`, ids.group, now, ids.owner]);
  await pool.query(`INSERT INTO community_media (filename,owner_id,created_at) VALUES ($1,$2,$3)`, [`fixture-media-${suffix}.png`, ids.owner, now]);
  return { ...ids, createdAt: now };
}

async function tableNames(pool, exclude = []) {
  const result = await pool.query(`SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename`);
  return result.rows.map(row => row.tablename).filter(name => !exclude.includes(name));
}

async function rowsSnapshot(pool, names) {
  const result = {};
  for (const name of [...names].sort()) {
    const table = quoteIdentifier(name);
    const rows = await pool.query(`SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), '[]'::jsonb) AS rows FROM public.${table} t`);
    result[name] = rows.rows[0].rows;
  }
  return JSON.stringify(result);
}

async function catalogSnapshot(pool, tableFilter = null) {
  const filter = tableFilter ? ' AND c.relname = ANY($1::text[])' : '';
  const params = tableFilter ? [tableFilter] : [];
  const [relations, columns, constraints, indexes, triggers, policies, sequences, routines] = await Promise.all([
    pool.query(`SELECT c.relname AS name,c.relkind AS kind,c.relrowsecurity AS rls,c.relforcerowsecurity AS force_rls FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p','v','m','S','f')${filter} ORDER BY c.relname`, params),
    pool.query(`SELECT table_name,ordinal_position,column_name,data_type,udt_name,is_nullable,column_default,is_generated,generation_expression,is_identity,identity_generation FROM information_schema.columns WHERE table_schema='public' ${tableFilter ? 'AND table_name = ANY($1::text[])' : ''} ORDER BY table_name,ordinal_position`, params),
    pool.query(`SELECT rel.relname AS table_name,con.conname AS name,con.contype AS type,pg_get_constraintdef(con.oid,true) AS definition FROM pg_constraint con JOIN pg_class rel ON rel.oid=con.conrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname='public' ${tableFilter ? 'AND rel.relname = ANY($1::text[])' : ''} ORDER BY rel.relname,con.conname`, params),
    pool.query(`SELECT tablename AS table_name,indexname AS name,regexp_replace(indexdef,' ON [^ ]+\\.',' ON <schema>.') AS definition FROM pg_indexes WHERE schemaname='public' ${tableFilter ? 'AND tablename = ANY($1::text[])' : ''} ORDER BY tablename,indexname`, params),
    pool.query(`SELECT rel.relname AS table_name,tg.tgname AS name,tg.tgenabled AS enabled,pg_get_triggerdef(tg.oid,true) AS definition FROM pg_trigger tg JOIN pg_class rel ON rel.oid=tg.tgrelid JOIN pg_namespace ns ON ns.oid=rel.relnamespace WHERE ns.nspname='public' AND NOT tg.tgisinternal ${tableFilter ? 'AND rel.relname = ANY($1::text[])' : ''} ORDER BY rel.relname,tg.tgname`, params),
    pool.query(`SELECT tablename,policyname,permissive,roles,cmd,qual,with_check FROM pg_policies WHERE schemaname='public' ${tableFilter ? 'AND tablename = ANY($1::text[])' : ''} ORDER BY tablename,policyname`, params),
    pool.query(`SELECT c.relname AS name,s.seqtypid::regtype::text AS data_type,s.seqstart,s.seqmin,s.seqmax,s.seqincrement,s.seqcycle FROM pg_sequence s JOIN pg_class c ON c.oid=s.seqrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' ${tableFilter ? 'AND c.relname = ANY($1::text[])' : ''} ORDER BY c.relname`, params),
    pool.query(`SELECT p.proname AS name,pg_get_functiondef(p.oid) AS definition FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' ${tableFilter ? "AND p.proname <> 'reject_admin_audit_mutation'" : ''} ORDER BY p.proname`)
  ]);
  return JSON.stringify({ relations: relations.rows, columns: columns.rows, constraints: constraints.rows, indexes: indexes.rows, triggers: triggers.rows, policies: policies.rows, sequences: sequences.rows, routines: routines.rows });
}

module.exports = { newId, assertPhaseBTestDatabase, createSyntheticExistingFixture, tableNames, rowsSnapshot, catalogSnapshot };
