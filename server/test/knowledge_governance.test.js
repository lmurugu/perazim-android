'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const G = require('../src/phase-b/knowledgeGovernance');

const UUIDS = ['00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000002','00000000-0000-4000-8000-000000000003','00000000-0000-4000-8000-000000000004'];
function pendingSource() { return { source_id: 'synthetic-source', version: 'v1', approval_status: 'UNVERIFIED',
  publication_status: 'UNPUBLISHED', created_by: 'submitter', reviewed_by: null, review_decision: null, approval_actor_id: null,
  supersedes_source_id: null, supersedes_version: null, conflict_group: null }; }
function clientFor(source = pendingSource()) {
  const calls = [];
  const events = [];
  const audits = [];
  return { calls, events, audits, source, async query(sql, params = []) {
    calls.push({ sql, params });
    if (/SELECT 1 FROM ai_knowledge_events/.test(sql)) return { rows: events.some(event => event.actor_id === params[0] && event.request_id === params[1]) ? [{ one: 1 }] : [] };
    if (/SELECT \* FROM ai_knowledge_sources/.test(sql)) return { rows: [source] };
    if (/INSERT INTO ai_knowledge_events/.test(sql)) { events.push({ actor_id: params[4], request_id: params[9], event_type: params[3] }); return { rows: [] }; }
    if (/INSERT INTO admin_audit_log/.test(sql)) { audits.push(params); return { rows: [] }; }
    if (/UPDATE ai_knowledge_sources/.test(sql)) {
      if (/approval_status=\$3/.test(sql)) { source.approval_status=params[2]; source.reviewed_by=params[3]; source.review_decision=params[4]; }
      if (/approval_status='APPROVED'/.test(sql)) { source.approval_status='APPROVED'; source.approval_actor_id=params[5]; source.approval_authority=params[2]; source.approval_reference=params[3]; }
      if (/publication_status='PUBLISHED'/.test(sql)) { source.publication_status='PUBLISHED'; }
      if (/approval_status='WITHDRAWN'/.test(sql)) { source.approval_status='WITHDRAWN'; source.publication_status='WITHDRAWN'; }
      return { rows: [] };
    }
    return { rows: [] };
  } };
}

async function authTests() {
  const req = { user: { id: 'pilot-user' }, token: 'auth', get: () => 'step-up' };
  const calls = [];
  const fakeClient = { async query(sql, params = []) {
    calls.push(sql);
    if (/FROM pilot_bootstrap/.test(sql)) return { rows: [{ user_id: 'pilot-user', security_version: 1, auth_not_before: 0, is_active: true }] };
    if (/FROM privileged_sessions/.test(sql)) return { rows: [{ token_hash: 'hash' }] };
    if (/FROM user_permissions/.test(sql)) return { rows: params[1] === 'knowledge.source.manage' ? [{ one: 1 }] : [] };
    return { rows: [] };
  } };
  await assert.rejects(G.authorize(fakeClient, req, 'knowledge.source.review'), error => error.code === 'PERMISSION_DENIED');
  await G.authorize(fakeClient, req, ['knowledge.source.manage','knowledge.source.review']);
  assert.equal(calls.some(sql => /pilot_bootstrap/.test(sql)), true, 'backend validates administrator bootstrap and step-up');
  assert.equal(calls.some(sql => /user_permissions/.test(sql)), true, 'backend checks an explicit active permission');
}

async function workflowTests() {
  const db = clientFor();
  await assert.rejects(G.approveSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'approver',
    requestId: UUIDS[0] }), error => error.code === 'APPROVAL_EVIDENCE_REQUIRED');
  await assert.rejects(G.approveSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'approver',
    approvalAuthority: 'Synthetic council', approvalReference: 'TEST-RESOLUTION-1', requestId: UUIDS[0] }), error => error.code === 'INVALID_SOURCE_TRANSITION');
  await assert.rejects(G.recordReview(clientFor(), { sourceId: 'synthetic-source', version: 'v1', actorId: 'submitter',
    decision: 'RECOMMEND_APPROVAL', reasonCode: 'SOURCE_REVIEW', requestId: UUIDS[1] }), error => error.code === 'SEPARATION_OF_DUTIES');
  const reviewed = await G.recordReview(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'reviewer',
    decision: 'RECOMMEND_APPROVAL', reasonCode: 'SOURCE_REVIEW', requestId: UUIDS[0], now: 100 });
  assert.equal(reviewed.approvalStatus, 'PENDING_APPROVAL');

  const approval = await G.approveSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'approver',
    approvalAuthority: 'Synthetic council', approvalReference: 'TEST-RESOLUTION-1', requestId: UUIDS[1], now: 200 });
  assert.equal(approval.approvalStatus, 'APPROVED');
  await assert.rejects(G.publishSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'approver', requestId: UUIDS[2] }), error => error.code === 'SEPARATION_OF_DUTIES');
  const published = await G.publishSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'publisher', requestId: UUIDS[2], now: 300 });
  assert.equal(published.publicationStatus, 'PUBLISHED');
  assert.equal(db.calls.some(call => /FOR UPDATE/.test(call.sql)), true, 'state changes lock source rows');
  await assert.rejects(G.publishSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'publisher', requestId: UUIDS[2] }), error => error.code === 'REQUEST_ALREADY_PROCESSED');
  const withdrawn = await G.withdrawSource(db, { sourceId: 'synthetic-source', version: 'v1', actorId: 'publisher',
    reasonCode: 'SOURCE_ERROR', requestId: UUIDS[3], now: 400 });
  assert.equal(withdrawn.publicationStatus, 'WITHDRAWN');
  assert.equal(db.events.length, 4, 'review, approval, publication and withdrawal events are append-only additions');
  assert.equal(db.audits.length, 4, 'every committed transition writes an audit record');
}

async function supersessionTests() {
  const oldSource = { source_id: 'same-source', version: 'v1', approval_status: 'APPROVED', publication_status: 'PUBLISHED',
    created_by: 'old-submitter', reviewed_by: 'old-reviewer', approval_actor_id: 'old-approver',
    supersedes_source_id: null, supersedes_version: null, conflict_group: null };
  const newSource = { source_id: 'same-source', version: 'v2', approval_status: 'APPROVED', publication_status: 'UNPUBLISHED',
    created_by: 'new-submitter', reviewed_by: 'new-reviewer', approval_actor_id: 'new-approver',
    supersedes_source_id: 'same-source', supersedes_version: 'v1', conflict_group: null };
  const records = new Map([['same-source:v1', oldSource], ['same-source:v2', newSource]]);
  const events = [];
  const sqlCalls = [];
  const client = { async query(sql, params = []) {
    sqlCalls.push(sql);
    if (/SELECT 1 FROM ai_knowledge_events/.test(sql)) return { rows: [] };
    if (/SELECT \* FROM ai_knowledge_sources/.test(sql)) return { rows: [records.get(`${params[0]}:${params[1]}`)] };
    if (/UPDATE ai_knowledge_sources SET approval_status='SUPERSEDED'/.test(sql)) {
      Object.assign(oldSource, { approval_status: 'SUPERSEDED', publication_status: 'SUPERSEDED' }); return { rows: [] };
    }
    if (/UPDATE ai_knowledge_sources SET publication_status='PUBLISHED'/.test(sql)) {
      newSource.publication_status='PUBLISHED'; return { rows: [] };
    }
    if (/INSERT INTO ai_knowledge_events/.test(sql)) { events.push(params[3]); return { rows: [] }; }
    return { rows: [] };
  } };
  const result = await G.publishSource(client, { sourceId: 'same-source', version: 'v2', actorId: 'publisher', requestId: UUIDS[0], now: 500 });
  assert.equal(result.supersededVersion, 'v1');
  assert.equal(oldSource.publication_status, 'SUPERSEDED');
  assert.equal(newSource.publication_status, 'PUBLISHED');
  assert.equal(events.length, 2, 'both sides of supersession have immutable events');
  assert.equal(sqlCalls.filter(sql => /FOR UPDATE/.test(sql)).length, 2, 'both versions are locked within the caller transaction');
}

async function main() {
  await authTests();
  await workflowTests();
  await supersessionTests();
  const migration = fs.readFileSync(path.resolve(__dirname, '../migrations/0014_ai_source_workflow_and_scripture.sql'), 'utf8');
  const priorMigration = fs.readFileSync(path.resolve(__dirname, '../migrations/0013_governed_ai_knowledge.sql'), 'utf8');
  assert.match(migration, /knowledge\.source\.approve\.institutional[^\n]+FALSE/);
  assert.match(migration, /CREATE UNIQUE INDEX ai_knowledge_one_published_version/);
  assert.match(migration, /ai_knowledge_reviews_no_row_mutation/);
  assert.match(priorMigration, /ai_knowledge_events_no_row_mutation/);
  assert.match(migration, /ai_knowledge_sources_no_delete/);
  assert.doesNotMatch(migration, /INSERT INTO user_permissions/i, 'migration assigns no governance grants');
  process.stdout.write('PASS: authorization and step-up gates, review/approval/publication separation, transition checks, replay rejection, withdrawal, audit writes, and database guard definitions.\n');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
