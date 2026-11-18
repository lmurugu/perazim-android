'use strict';

const { isPostgresActive, withTransaction } = require('../db');
const { PhaseBError, requireAdminAuthority, userPermission, writeAudit, newId } = require('./security');

const REASONS = new Set(['SOURCE_REVIEW', 'SOURCE_APPROVAL', 'SOURCE_PUBLICATION', 'SOURCE_WITHDRAWAL', 'SOURCE_SUPERSESSION', 'CONFLICT_CLASSIFICATION', 'SCRIPTURE_IMPORT', 'SCRIPTURE_PUBLICATION', 'ADMIN_READ']);
const CONFLICT_CLASSES = new Set(['AUTHORITY_SCOPE', 'TEXTUAL_CONTRADICTION', 'VERSION', 'INCOMPLETE_SOURCE', 'OTHER']);
const WITHDRAWAL_REASONS = new Set(['SOURCE_ERROR', 'AUTHORITY_WITHDRAWAL', 'LICENSING_CHANGE', 'REPLACED', 'SECURITY_REVIEW']);
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function fail(status, code, message) { throw new PhaseBError(status, code, message); }
function requireUuid(value, code = 'INVALID_REQUEST_ID') {
  if (typeof value !== 'string' || !UUID_RE.test(value)) fail(400, code, 'A valid request identifier is required.');
  return value;
}
function requireReason(value, allowed = REASONS) {
  if (typeof value !== 'string' || !allowed.has(value)) fail(400, 'INVALID_REASON', 'A permitted reason code is required.');
  return value;
}

async function withGovernanceTransaction(work) {
  if (!isPostgresActive()) fail(503, 'KNOWLEDGE_STORE_UNAVAILABLE', 'The governed knowledge store is unavailable.');
  return withTransaction(work);
}

async function authorize(client, req, capability) {
  const privilegedToken = req.get ? req.get('X-Privileged-Session') : req.privilegedToken;
  if (!privilegedToken) fail(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required.');
  const permissions = Array.isArray(capability) ? capability : null;
  await requireAdminAuthority(client, {
    userId: req.user.id, authToken: req.token, privilegedToken,
    capability: permissions ? null : capability, scope: 'global', lock: true
  });
  if (permissions) {
    const grants = await Promise.all(permissions.map(code => userPermission(client, req.user.id, code, 'global', Date.now(), true)));
    if (!grants.some(Boolean)) fail(403, 'PERMISSION_DENIED', 'This action is not permitted.');
  }
}

async function runGovernanceAction(req, capability, work) {
  return withGovernanceTransaction(async client => {
    await authorize(client, req, capability);
    return work(client);
  });
}

async function getSourceForUpdate(client, sourceId, version) {
  const result = await client.query(
    `SELECT * FROM ai_knowledge_sources WHERE source_id=$1 AND version=$2 FOR UPDATE`, [sourceId, version]
  );
  if (!result.rows.length) fail(404, 'SOURCE_NOT_FOUND', 'Source was not found.');
  return result.rows[0];
}

async function assertFreshRequest(client, actorId, requestId) {
  const prior = await client.query(
    'SELECT 1 FROM ai_knowledge_events WHERE actor_id=$1 AND request_id=$2 LIMIT 1', [actorId, requestId]
  );
  if (prior.rows.length) fail(409, 'REQUEST_ALREADY_PROCESSED', 'This request has already been processed.');
}

async function recordEvent(client, { sourceId, version, type, actorId, role, authorityReference = null, reason, requestId, now }) {
  await client.query(
    `INSERT INTO ai_knowledge_events
      (event_id,source_id,source_version,event_type,actor_id,actor_role,authority_reference,reason,created_at,request_id)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
    [newId(), sourceId, version, type, actorId, role, authorityReference, reason, now, requestId]
  );
}

async function recordReview(client, { sourceId, version, actorId, decision, reasonCode, requestId, now = Date.now() }) {
  requireUuid(requestId);
  if (!['RECOMMEND_APPROVAL', 'REJECT'].includes(decision)) fail(400, 'INVALID_REVIEW_DECISION', 'A supported review decision is required.');
  requireReason(reasonCode, new Set(['SOURCE_REVIEW', 'SOURCE_REJECTED']));
  await assertFreshRequest(client, actorId, requestId);
  const source = await getSourceForUpdate(client, sourceId, version);
  if (!(source.approval_status === 'UNVERIFIED' || (source.approval_status === 'DRAFT' && source.reviewed_by == null)) || source.publication_status !== 'UNPUBLISHED') {
    fail(409, 'INVALID_SOURCE_TRANSITION', 'Only an unpublished, unverified source can be reviewed.');
  }
  if (!source.created_by || source.created_by === actorId) fail(403, 'SEPARATION_OF_DUTIES', 'A source submitter cannot review the same source.');
  const status = decision === 'RECOMMEND_APPROVAL' ? 'PENDING_APPROVAL' : 'DRAFT';
  await client.query(
    `INSERT INTO ai_knowledge_source_reviews (review_id,source_id,source_version,reviewer_id,decision,reason_code,request_id,created_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
    [newId(), sourceId, version, actorId, decision, reasonCode, requestId, now]
  );
  await client.query(
    `UPDATE ai_knowledge_sources SET approval_status=$3,reviewed_by=$4,review_decision=$5,reviewed_at=$6,updated_at=$6
      WHERE source_id=$1 AND version=$2`, [sourceId, version, status, actorId, decision, now]
  );
  await recordEvent(client, { sourceId, version, type: 'REVIEWED', actorId, role: 'SOURCE_REVIEWER', reason: reasonCode, requestId, now });
  await writeAudit(client, { actorId, action: 'AI_SOURCE_REVIEW', resourceType: 'ai_knowledge_source', resourceId: `${sourceId}:${version}`, reason: reasonCode, requestId, createdAt: now });
  return { sourceId, version, approvalStatus: status, reviewDecision: decision, reviewedAt: now };
}

async function approveSource(client, { sourceId, version, actorId, approvalAuthority, approvalReference, requestId, now = Date.now() }) {
  requireUuid(requestId);
  if (typeof approvalAuthority !== 'string' || !approvalAuthority.trim() || approvalAuthority.length > 200 ||
      typeof approvalReference !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._:/# -]{3,199}$/.test(approvalReference.trim())) {
    fail(400, 'APPROVAL_EVIDENCE_REQUIRED', 'A documented approving authority and reference are required.');
  }
  await assertFreshRequest(client, actorId, requestId);
  const source = await getSourceForUpdate(client, sourceId, version);
  if (source.approval_status !== 'PENDING_APPROVAL' || source.review_decision !== 'RECOMMEND_APPROVAL' ||
      source.publication_status !== 'UNPUBLISHED') fail(409, 'INVALID_SOURCE_TRANSITION', 'A source must pass independent review before approval.');
  if (!source.created_by || source.created_by === actorId || source.reviewed_by === actorId) {
    fail(403, 'SEPARATION_OF_DUTIES', 'The submitter and reviewer cannot provide the institutional approval.');
  }
  await client.query(
    `UPDATE ai_knowledge_sources SET approval_status='APPROVED',approval_authority=$3,approval_reference=$4,
       approved_at=$5,approval_actor_id=$6,updated_at=$5 WHERE source_id=$1 AND version=$2`,
    [sourceId, version, approvalAuthority.trim(), approvalReference.trim(), now, actorId]
  );
  await recordEvent(client, { sourceId, version, type: 'APPROVED', actorId, role: 'INSTITUTIONAL_APPROVER', authorityReference: approvalReference.trim(), reason: 'SOURCE_APPROVAL', requestId, now });
  await writeAudit(client, { actorId, action: 'AI_SOURCE_APPROVAL', resourceType: 'ai_knowledge_source', resourceId: `${sourceId}:${version}`, reason: 'SOURCE_APPROVAL', requestId, createdAt: now });
  return { sourceId, version, approvalStatus: 'APPROVED', approvedAt: now, authorityReference: approvalReference.trim() };
}

async function publishSource(client, { sourceId, version, actorId, requestId, now = Date.now() }) {
  requireUuid(requestId);
  await assertFreshRequest(client, actorId, requestId);
  const source = await getSourceForUpdate(client, sourceId, version);
  if (source.approval_status !== 'APPROVED' || source.publication_status !== 'UNPUBLISHED' || !source.approval_actor_id) {
    fail(409, 'INVALID_SOURCE_TRANSITION', 'Only an approved, unpublished source can be published.');
  }
  if ([source.created_by, source.reviewed_by, source.approval_actor_id].includes(actorId)) {
    fail(403, 'SEPARATION_OF_DUTIES', 'The submitter, reviewer or doctrinal approver cannot publish this source.');
  }
  let superseded = null;
  if (source.supersedes_source_id || source.supersedes_version) {
    if (!source.supersedes_source_id || !source.supersedes_version || source.supersedes_source_id !== source.source_id || source.supersedes_version === source.version) {
      fail(409, 'INVALID_SUPERSESSION_LINK', 'A replacement must reference an earlier version of the same source.');
    }
    superseded = await getSourceForUpdate(client, source.supersedes_source_id, source.supersedes_version);
    if (superseded.approval_status !== 'APPROVED' || superseded.publication_status !== 'PUBLISHED') {
      fail(409, 'INVALID_SUPERSESSION_TARGET', 'The source being superseded must be currently published.');
    }
    await client.query(
      `UPDATE ai_knowledge_sources SET approval_status='SUPERSEDED',publication_status='SUPERSEDED',
       superseded_by_source_id=$3,superseded_by_version=$4,updated_at=$5 WHERE source_id=$1 AND version=$2`,
      [superseded.source_id, superseded.version, source.source_id, source.version, now]
    );
    await recordEvent(client, { sourceId: superseded.source_id, version: superseded.version, type: 'SUPERSEDED', actorId, role: 'PUBLICATION_EXECUTOR', reason: 'SOURCE_SUPERSESSION', requestId, now });
  }
  await client.query(
    `UPDATE ai_knowledge_sources SET publication_status='PUBLISHED',published_by=$3,published_at=$4,updated_at=$4
      WHERE source_id=$1 AND version=$2 AND publication_status='UNPUBLISHED'`, [sourceId, version, actorId, now]
  );
  await recordEvent(client, { sourceId, version, type: 'PUBLISHED', actorId, role: 'PUBLICATION_EXECUTOR', reason: superseded ? 'SOURCE_SUPERSESSION' : 'SOURCE_PUBLICATION', requestId, now });
  await writeAudit(client, { actorId, action: 'AI_SOURCE_PUBLISH', resourceType: 'ai_knowledge_source', resourceId: `${sourceId}:${version}`, reason: superseded ? 'SOURCE_SUPERSESSION' : 'SOURCE_PUBLICATION', requestId, createdAt: now });
  return { sourceId, version, publicationStatus: 'PUBLISHED', supersededVersion: superseded && superseded.version, publishedAt: now };
}

async function withdrawSource(client, { sourceId, version, actorId, reasonCode, requestId, now = Date.now() }) {
  requireUuid(requestId);
  requireReason(reasonCode, WITHDRAWAL_REASONS);
  await assertFreshRequest(client, actorId, requestId);
  const source = await getSourceForUpdate(client, sourceId, version);
  if (source.approval_status !== 'APPROVED' || source.publication_status !== 'PUBLISHED') fail(409, 'INVALID_SOURCE_TRANSITION', 'Only a currently published source can be withdrawn.');
  await client.query(
    `UPDATE ai_knowledge_sources SET approval_status='WITHDRAWN',publication_status='WITHDRAWN',
       withdrawn_at=$3,withdrawal_reason=$4,withdrawn_by=$5,updated_at=$3 WHERE source_id=$1 AND version=$2`,
    [sourceId, version, now, reasonCode, actorId]
  );
  await recordEvent(client, { sourceId, version, type: 'WITHDRAWN', actorId, role: 'PUBLICATION_EXECUTOR', reason: reasonCode, requestId, now });
  await writeAudit(client, { actorId, action: 'AI_SOURCE_WITHDRAW', resourceType: 'ai_knowledge_source', resourceId: `${sourceId}:${version}`, reason: reasonCode, requestId, createdAt: now });
  return { sourceId, version, publicationStatus: 'WITHDRAWN', withdrawnAt: now };
}

async function classifySourceConflict(client, { sources, actorId, classification, classificationReference, requestId, now = Date.now() }) {
  requireUuid(requestId);
  if (!Array.isArray(sources) || sources.length < 2 || sources.length > 6) fail(400, 'INVALID_CONFLICT_SET', 'A conflict must identify 2 to 6 source versions.');
  if (!['AUTHORITY_SCOPE', 'TEXTUAL_CONTRADICTION', 'VERSION', 'INCOMPLETE_SOURCE', 'OTHER'].includes(classification)) fail(400, 'INVALID_CONFLICT_CLASS', 'A supported conflict classification is required.');
  if (typeof classificationReference !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._:/# -]{3,199}$/.test(classificationReference.trim())) fail(400, 'CONFLICT_REFERENCE_REQUIRED', 'A review reference is required.');
  await assertFreshRequest(client, actorId, requestId);
  const unique = new Set(sources.map(s => `${s.sourceId}\0${s.version}`));
  if (unique.size !== sources.length) fail(400, 'INVALID_CONFLICT_SET', 'A source version may appear only once.');
  const rows = [];
  for (const entry of [...sources].sort((a, b) => `${a.sourceId}:${a.version}`.localeCompare(`${b.sourceId}:${b.version}`))) {
    const row = await getSourceForUpdate(client, entry.sourceId, entry.version);
    if (row.approval_status !== 'APPROVED' || row.publication_status !== 'PUBLISHED' || row.conflict_group) fail(409, 'INVALID_CONFLICT_SOURCE', 'Every source must be published and not already assigned to a conflict group.');
    if (row.created_by === actorId) fail(403, 'SEPARATION_OF_DUTIES', 'A source submitter cannot classify its conflict.');
    rows.push(row);
  }
  const conflictGroup = newId();
  await client.query(
    `INSERT INTO ai_knowledge_conflict_groups (conflict_group,classification,status,classification_reference,created_by,created_at)
     VALUES ($1,$2,'UNRESOLVED',$3,$4,$5)`, [conflictGroup, classification, classificationReference.trim(), actorId, now]
  );
  for (const row of rows) {
    await client.query(
      `INSERT INTO ai_knowledge_conflict_sources (conflict_group,source_id,source_version) VALUES ($1,$2,$3)`,
      [conflictGroup, row.source_id, row.version]
    );
    await client.query(
      `UPDATE ai_knowledge_sources SET conflict_group=$3,updated_at=$4 WHERE source_id=$1 AND version=$2`,
      [row.source_id, row.version, conflictGroup, now]
    );
    await recordEvent(client, { sourceId: row.source_id, version: row.version, type: 'CONFLICT_CLASSIFIED', actorId, role: 'SOURCE_REVIEWER', authorityReference: classificationReference.trim(), reason: 'CONFLICT_CLASSIFICATION', requestId, now });
  }
  await writeAudit(client, { actorId, action: 'AI_SOURCE_CONFLICT_CLASSIFY', resourceType: 'ai_knowledge_conflict_group', resourceId: conflictGroup, reason: 'CONFLICT_CLASSIFICATION', requestId, createdAt: now });
  return { conflictGroup, classification, status: 'UNRESOLVED', sources: sources.map(s => ({ sourceId: s.sourceId, version: s.version })) };
}

module.exports = {
  REASONS, CONFLICT_CLASSES, WITHDRAWAL_REASONS, UUID_RE,
  requireUuid, requireReason, withGovernanceTransaction, authorize, runGovernanceAction,
  recordReview, approveSource, publishSource, withdrawSource, classifySourceConflict
};
