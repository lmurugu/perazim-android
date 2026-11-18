'use strict';

const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { PhaseBError, writeAudit, newId } = require('../phase-b/security');
const { ingestSource } = require('../phase-b/governedKnowledge');
const governance = require('../phase-b/knowledgeGovernance');
const scripture = require('../phase-b/scriptureCorpus');

const router = express.Router();
router.use((req, res, next) => { res.set('Cache-Control', 'private, no-store'); res.set('Pragma', 'no-cache'); next(); });
router.use(requireAuth);

function bad(status, code, message) { throw new PhaseBError(status, code, message); }
function requiredText(value, name, max = 300) {
  if (typeof value !== 'string' || !value.trim() || value.length > max) bad(400, 'INVALID_REQUEST', `${name} is required.`);
  return value.trim();
}
function requestId(body) { return governance.requireUuid(body && body.requestId); }
function handle(error, res) {
  if (error instanceof PhaseBError) return res.status(error.status).json({ error: error.message, code: error.code });
  if (error instanceof scripture.CorpusError) {
    const status = ['CORPUS_NOT_FOUND'].includes(error.code) ? 404 : ['SEPARATION_OF_DUTIES'].includes(error.code) ? 403 : 400;
    return res.status(status).json({ error: error.message, code: error.code });
  }
  if (error instanceof require('../phase-b/governedKnowledge').KnowledgeError) return res.status(400).json({ error: error.message, code: error.code });
  if (error && error.code === '23505') return res.status(409).json({ error: 'Request conflicts with existing data.', code: 'CONFLICT' });
  return res.status(500).json({ error: 'Request could not be completed.', code: 'INTERNAL_ERROR' });
}
async function action(req, capability, work) {
  return governance.runGovernanceAction(req, capability, work);
}

router.post('/', async (req, res) => {
  try {
    const body = req.body || {};
    const id = requestId(body);
    const metadata = body.metadata;
    if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) bad(400, 'INVALID_REQUEST', 'Source metadata is required.');
    if (typeof body.text !== 'string' || !body.text.trim() || body.text.length > 2_000_000) bad(400, 'INVALID_REQUEST', 'Source text must be 1 to 2,000,000 characters.');
    const sourceMetadata = {
      ...metadata,
      sourceId: requiredText(metadata.sourceId, 'sourceId', 200),
      version: requiredText(metadata.version, 'version', 100),
      confidentialityReviewer: req.user.id,
      confidentialityReviewedAt: Date.now(),
      status: 'UNVERIFIED', approvalAuthority: null, approvalReference: null
    };
    const result = await action(req, 'knowledge.source.manage', async client => {
      const replay = await client.query('SELECT 1 FROM ai_knowledge_events WHERE actor_id=$1 AND request_id=$2 LIMIT 1', [req.user.id, id]);
      if (replay.rows.length) bad(409, 'REQUEST_ALREADY_PROCESSED', 'This request has already been processed.');
      const registered = await ingestSource(client, sourceMetadata, body.text, { actorId: req.user.id, requestId: id });
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SOURCE_REGISTER', resourceType: 'ai_knowledge_source', resourceId: `${sourceMetadata.sourceId}:${sourceMetadata.version}`, reason: 'SOURCE_REGISTRATION', requestId: id });
      return registered;
    });
    return res.status(201).json(result);
  } catch (error) { return handle(error, res); }
});

router.get('/', async (req, res) => {
  try {
    const rows = await action(req, ['knowledge.source.manage','knowledge.source.review'], async client => {
      const result = await client.query(
        `SELECT source_id,version,title,document_type,issuing_authority,authority_class,scope_of_authority,
              approval_status,publication_status,approval_authority,approval_reference,approved_at,
              reviewed_by,review_decision,reviewed_at,published_by,published_at,withdrawn_at,withdrawal_reason,
              conflict_group,content_hash,ingested_at,created_by
         FROM ai_knowledge_sources ORDER BY ingested_at DESC,source_id,version LIMIT 500`);
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SOURCE_LIST_READ', resourceType: 'ai_knowledge_source', reason: 'ADMIN_READ' });
      return result.rows;
    });
    return res.json({ sources: rows });
  } catch (error) { return handle(error, res); }
});

router.get('/:sourceId/:version', async (req, res) => {
  try {
    const result = await action(req, ['knowledge.source.manage','knowledge.source.review'], async client => {
      const source = await client.query('SELECT * FROM ai_knowledge_sources WHERE source_id=$1 AND version=$2', [req.params.sourceId, req.params.version]);
      if (!source.rows.length) bad(404, 'SOURCE_NOT_FOUND', 'Source was not found.');
      const chunks = await client.query('SELECT chunk_id,section,location,content,content_hash FROM ai_knowledge_chunks WHERE source_id=$1 AND source_version=$2 ORDER BY section,chunk_id', [req.params.sourceId, req.params.version]);
      const integrity = chunks.rows.every(chunk => require('../phase-b/governedKnowledge').sha256(chunk.content) === chunk.content_hash);
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SOURCE_DETAIL_READ', resourceType: 'ai_knowledge_source', resourceId: `${req.params.sourceId}:${req.params.version}`, reason: 'ADMIN_READ' });
      return { source: source.rows[0], chunks: chunks.rows, chunkIntegrityValid: integrity };
    });
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

router.get('/:sourceId/:version/history', async (req, res) => {
  try {
    const events = await action(req, ['knowledge.source.manage','knowledge.source.review'], async client => {
      const exists = await client.query('SELECT 1 FROM ai_knowledge_sources WHERE source_id=$1 AND version=$2', [req.params.sourceId, req.params.version]);
      if (!exists.rows.length) bad(404, 'SOURCE_NOT_FOUND', 'Source was not found.');
      const [lifecycle, reviews] = await Promise.all([
        client.query(`SELECT event_type,actor_id,actor_role,authority_reference,reason,created_at,request_id
          FROM ai_knowledge_events WHERE source_id=$1 AND source_version=$2 ORDER BY created_at,event_id`, [req.params.sourceId, req.params.version]),
        client.query(`SELECT reviewer_id,decision,reason_code,request_id,created_at
          FROM ai_knowledge_source_reviews WHERE source_id=$1 AND source_version=$2 ORDER BY created_at,review_id`, [req.params.sourceId, req.params.version])
      ]);
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SOURCE_HISTORY_READ', resourceType: 'ai_knowledge_source', resourceId: `${req.params.sourceId}:${req.params.version}`, reason: 'ADMIN_READ' });
      return [
        ...lifecycle.rows.map(event => ({ type: 'LIFECYCLE', ...event })),
        ...reviews.rows.map(review => ({ type: 'REVIEW', actor_id: review.reviewer_id, ...review }))
      ].sort((a, b) => Number(a.created_at) - Number(b.created_at));
    });
    return res.json({ events });
  } catch (error) { return handle(error, res); }
});

router.post('/:sourceId/:version/review', async (req, res) => {
  try {
    const result = await action(req, 'knowledge.source.review', client => governance.recordReview(client, {
      sourceId: req.params.sourceId, version: req.params.version, actorId: req.user.id,
      decision: req.body?.decision, reasonCode: req.body?.reasonCode, requestId: requestId(req.body)
    }));
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/:sourceId/:version/approve', async (req, res) => {
  try {
    const result = await action(req, 'knowledge.source.approve.institutional', client => governance.approveSource(client, {
      sourceId: req.params.sourceId, version: req.params.version, actorId: req.user.id,
      approvalAuthority: req.body?.approvalAuthority, approvalReference: req.body?.approvalReference,
      requestId: requestId(req.body)
    }));
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/:sourceId/:version/publish', async (req, res) => {
  try {
    const result = await action(req, 'knowledge.source.publish', client => governance.publishSource(client, {
      sourceId: req.params.sourceId, version: req.params.version, actorId: req.user.id, requestId: requestId(req.body)
    }));
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/:sourceId/:version/withdraw', async (req, res) => {
  try {
    const result = await action(req, 'knowledge.source.publish', client => governance.withdrawSource(client, {
      sourceId: req.params.sourceId, version: req.params.version, actorId: req.user.id,
      reasonCode: req.body?.reasonCode, requestId: requestId(req.body)
    }));
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/conflicts', async (req, res) => {
  try {
    const result = await action(req, 'knowledge.source.review', client => governance.classifySourceConflict(client, {
      sources: req.body?.sources, actorId: req.user.id, classification: req.body?.classification,
      classificationReference: req.body?.classificationReference, requestId: requestId(req.body)
    }));
    return res.status(201).json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/scripture/corpora', async (req, res) => {
  try {
    if (process.env.PERAZIM_AI_SCRIPTURE_CORPUS_IMPORT_ENABLED !== 'true') bad(503, 'SCRIPTURE_IMPORT_DISABLED', 'Authorized Scripture corpus import is disabled pending verified rights evidence.');
    const body = req.body || {};
    const id = requestId(body);
    const result = await action(req, 'knowledge.scripture.import', async client => {
      const prior = await client.query('SELECT 1 FROM ai_scripture_corpus_events WHERE actor_id=$1 AND request_id=$2 LIMIT 1', [req.user.id, id]);
      if (prior.rows.length) bad(409, 'REQUEST_ALREADY_PROCESSED', 'This request has already been processed.');
      const imported = await scripture.importCorpus(client, { manifest: body.manifest, books: body.books, verses: body.verses }, { actorId: req.user.id });
      await client.query(`INSERT INTO ai_scripture_corpus_events(event_id,corpus_id,event_type,actor_id,request_id,reason,created_at)
        VALUES($1,$2,'IMPORTED',$3,$4,'SCRIPTURE_IMPORT',$5)`, [newId(), imported.corpusId, req.user.id, id, Date.now()]);
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SCRIPTURE_IMPORT', resourceType: 'ai_scripture_corpus', resourceId: imported.corpusId, reason: 'SCRIPTURE_IMPORT', requestId: id });
      return imported;
    });
    return res.status(201).json(result);
  } catch (error) { return handle(error, res); }
});

router.post('/scripture/corpora/:corpusId/publish', async (req, res) => {
  try {
    if (process.env.PERAZIM_AI_SCRIPTURE_CORPUS_IMPORT_ENABLED !== 'true') bad(503, 'SCRIPTURE_IMPORT_DISABLED', 'Authorized Scripture corpus publication is disabled pending verified rights evidence.');
    const id = requestId(req.body);
    const result = await action(req, 'knowledge.scripture.publish', async client => {
      const prior = await client.query('SELECT 1 FROM ai_scripture_corpus_events WHERE actor_id=$1 AND request_id=$2 LIMIT 1', [req.user.id, id]);
      if (prior.rows.length) bad(409, 'REQUEST_ALREADY_PROCESSED', 'This request has already been processed.');
      const published = await scripture.publishCorpus(client, { corpusId: req.params.corpusId, actorId: req.user.id, requestId: id });
      for (const retiredCorpusId of published.retiredCorpusIds || []) {
        await client.query(`INSERT INTO ai_scripture_corpus_events(event_id,corpus_id,event_type,actor_id,request_id,reason,created_at)
          VALUES($1,$2,'RETIRED',$3,$4,'SCRIPTURE_REPLACED',$5)`, [newId(), retiredCorpusId, req.user.id, id, Date.now()]);
      }
      await client.query(`INSERT INTO ai_scripture_corpus_events(event_id,corpus_id,event_type,actor_id,request_id,reason,created_at)
        VALUES($1,$2,'PUBLISHED',$3,$4,'SCRIPTURE_PUBLICATION',$5)`, [newId(), published.corpusId, req.user.id, id, Date.now()]);
      await writeAudit(client, { actorId: req.user.id, action: 'AI_SCRIPTURE_PUBLISH', resourceType: 'ai_scripture_corpus', resourceId: published.corpusId, reason: 'SCRIPTURE_PUBLICATION', requestId: id });
      return published;
    });
    return res.json(result);
  } catch (error) { return handle(error, res); }
});

module.exports = router;
