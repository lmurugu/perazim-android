'use strict';

const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { isPostgresActive, memoryStore, query, withTransaction } = require('../db');
const { requireAuth } = require('../middleware/auth');
const { PhaseBError, newId, requireAdminAuthority } = require('../phase-b/security');
const { ACKNOWLEDGEMENT_TEXT, getCurrentNotice, recordAcknowledgement } = require('../phase-b/privacyNotice');
const { recordConsentChoice } = require('../phase-b/data01Governance');

const router = express.Router();
const PROJECT_ROOT = path.resolve(__dirname, '../../..');
const PUBLIC_POLICY_ROOT = path.resolve(PROJECT_ROOT, 'docs/data-protection/public');
const POLICY_UNAVAILABLE = Object.freeze({ error: 'Published privacy document unavailable' });
const RECEIPT_TTL_MS = 10 * 60 * 1000;

function publicationIsCurrent(document) {
  return document && document.publication_status === 'PUBLISHED' &&
    Number.isSafeInteger(Number(document.effective_at)) && Number(document.effective_at) > 0 &&
    Number(document.effective_at) <= Date.now() && !document.withdrawn_at;
}

function verifiedPublicContent(document, content) {
  const digest = require('crypto').createHash('sha256').update(content, 'utf8').digest('hex');
  return publicationIsCurrent(document) && digest === String(document.content_hash || '').trim();
}

router.get('/notice/current', async (req, res) => {
  try {
    const notice = await getCurrentNotice();
    if (!publicationIsCurrent(notice)) {
      return res.status(503).json({ error: 'Published privacy document unavailable' });
    }
    return res.json({
      version: notice.version,
      sha256: notice.content_hash,
      content: notice.content,
      publication_status: notice.publication_status,
      effective_at: notice.effective_at,
      presented_at: notice.presented_at
    });
  } catch (_) {
    return res.status(503).json({ error: 'Published privacy document unavailable' });
  }
});

router.get('/public-policy', async (req, res) => {
  try {
    let policy;
    if (!isPostgresActive() && process.env.NODE_ENV === 'test') {
      policy = memoryStore.data01TestPublicPolicy || null;
    } else {
      const result = await query(
        `SELECT version,content_hash,document_path,publication_status,effective_at,withdrawn_at
           FROM privacy_policy_versions WHERE document_kind='PUBLIC_POLICY' AND is_active=TRUE LIMIT 1`
      );
      policy = result.rows[0] || null;
    }
    if (!publicationIsCurrent(policy)) {
      return res.status(503).json(POLICY_UNAVAILABLE);
    }
    let content;
    if (typeof policy.content === 'string') {
      content = policy.content;
    } else {
      const documentPath = path.resolve(PROJECT_ROOT, policy.document_path);
      const relative = path.relative(PUBLIC_POLICY_ROOT, documentPath);
      if (relative.startsWith('..') || path.isAbsolute(relative)) throw new Error('Published policy path is outside the public policy directory');
      content = fs.readFileSync(documentPath, 'utf8');
    }
    if (!verifiedPublicContent(policy, content)) {
      return res.status(503).json(POLICY_UNAVAILABLE);
    }
    return res.type('text/markdown; charset=utf-8').send(content);
  } catch (_) {
    return res.status(503).json(POLICY_UNAVAILABLE);
  }
});

router.post('/presentation', requireAuth, async (req, res) => {
  try {
    const notice = await getCurrentNotice();
    if (!publicationIsCurrent(notice)) return res.status(503).json({ error: 'Published privacy document unavailable' });
    const existingAck = isPostgresActive()
      ? await query(`SELECT 1 FROM privacy_notice_acknowledgements WHERE user_id=$1 AND notice_version=$2 AND content_hash=$3 LIMIT 1`, [req.user.id, notice.version, notice.content_hash])
      : { rows: memoryStore.privacy_notice_acknowledgements.filter(row => row.user_id === req.user.id && row.notice_version === notice.version && row.content_hash === notice.content_hash) };
    if (existingAck.rows.length) return res.json({ acknowledged: true, version: notice.version, sha256: notice.content_hash });
    const receipt = crypto.randomBytes(32).toString('base64url');
    const receiptHash = crypto.createHash('sha256').update(receipt).digest('hex');
    const presentedAt = Date.now();
    await withTransaction(client => client.query(
      `INSERT INTO privacy_presentation_receipts
        (receipt_hash,user_id,notice_version,content_hash,presented_at,expires_at)
       VALUES ($1,$2,$3,$4,$5,$6)`,
      [receiptHash, req.user.id, notice.version, notice.content_hash, presentedAt, presentedAt + RECEIPT_TTL_MS]
    ));
    return res.json({ version: notice.version, sha256: notice.content_hash, content: notice.content, presented_at: presentedAt, receipt });
  } catch (_) {
    return res.status(503).json({ error: 'Published privacy document unavailable' });
  }
});

router.post('/acknowledge', requireAuth, async (req, res) => {
  try {
    const receipt = typeof req.body?.receipt === 'string' ? req.body.receipt : '';
    if (receipt.length < 32 || receipt.length > 128) return res.status(428).json({ code: 'PRIVACY_NOTICE_ACK_REQUIRED' });
    const notice = await getCurrentNotice();
    if (!publicationIsCurrent(notice)) return res.status(428).json({ code: 'PRIVACY_NOTICE_ACK_REQUIRED' });
    const receiptHash = crypto.createHash('sha256').update(receipt).digest('hex');
    const acknowledgedAt = Date.now();
    await withTransaction(async client => {
      const found = await client.query(
        `SELECT receipt_hash,user_id,notice_version,content_hash,presented_at,expires_at
           FROM privacy_presentation_receipts
          WHERE receipt_hash=$1 AND user_id=$2 AND consumed_at IS NULL AND expires_at>$3 FOR UPDATE`,
        [receiptHash, req.user.id, acknowledgedAt]
      );
      const row = found.rows[0];
      if (!row || row.notice_version !== notice.version || row.content_hash !== notice.content_hash || Number(row.expires_at) <= acknowledgedAt) {
        const error = new Error('Invalid privacy presentation receipt'); error.code = 'INVALID_RECEIPT'; throw error;
      }
      const current = await client.query(
        `SELECT 1 FROM privacy_notice_versions
          WHERE version=$1 AND content_hash=$2 AND is_active=TRUE AND publication_status='PUBLISHED' AND withdrawn_at IS NULL AND effective_at <= $3 LIMIT 1`,
        [notice.version, notice.content_hash, acknowledgedAt]
      );
      if (!current.rows.length && isPostgresActive()) {
        const error = new Error('Notice no longer current'); error.code = 'INVALID_RECEIPT'; throw error;
      }
      const consumed = await client.query(
        `UPDATE privacy_presentation_receipts SET consumed_at=$1
          WHERE receipt_hash=$2 AND user_id=$3 AND consumed_at IS NULL RETURNING presented_at`,
        [acknowledgedAt, receiptHash, req.user.id]
      );
      if (!consumed.rows.length) { const error = new Error('Receipt already consumed'); error.code = 'INVALID_RECEIPT'; throw error; }
      await recordAcknowledgement(client, req.user.id, { ...notice, presented_at: Number(row.presented_at) }, acknowledgedAt);
    });
    return res.json({ success: true, version: notice.version, sha256: notice.content_hash });
  } catch (error) {
    if (error && error.code === 'INVALID_RECEIPT') return res.status(428).json({ code: 'PRIVACY_NOTICE_ACK_REQUIRED' });
    return res.status(503).json({ error: 'Privacy acknowledgement could not be recorded.' });
  }
});

router.post('/requests', requireAuth, async (req, res) => {
  const requestType = req.body?.request_type;
  const requestId = typeof req.body?.request_id === 'string' ? req.body.request_id : '';
  if (!['ACCESS', 'CORRECTION', 'ERASURE'].includes(requestType) || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(requestId)) {
    return res.status(400).json({ error: 'A valid request type and request identifier are required.' });
  }
  const id = newId();
  const now = Date.now();
  try {
    let status = 'PENDING';
    let reasonCode = null;
    if (requestType === 'ERASURE' && isPostgresActive()) {
      const hold = await query(`SELECT 1 FROM legal_holds WHERE subject_ref=$1 AND status='ACTIVE' LIMIT 1`, [req.user.id]);
      if (hold.rows.length) { status = 'DEFERRED'; reasonCode = 'ACTIVE_HOLD'; }
    }
    if (isPostgresActive()) {
      await withTransaction(async client => {
        await client.query(`INSERT INTO data_subject_requests (id,request_id,requester_id,request_type,status,decision_reason_code,submitted_at,updated_at) VALUES ($1,$2,$3,$4,$5,$6,$7,$7)`,
          [id, requestId, req.user.id, requestType, status, reasonCode, now]);
        await client.query(`INSERT INTO data_subject_request_events (id,request_id,actor_id,event,reason_code,created_at) VALUES ($1,$2,$3,$4,$5,$6)`,
          [newId(), id, req.user.id, status === 'DEFERRED' ? 'DEFERRED' : 'SUBMITTED', reasonCode || 'REQUEST_RECEIVED', now]);
        await client.query(`INSERT INTO data_subject_request_revisions (request_id,revision,request_type,actor_id,created_at) VALUES ($1,1,$2,$3,$4)`,
          [id, requestType, req.user.id, now]);
      });
    } else {
      memoryStore.data_subject_requests.push({ id, request_id: requestId, requester_id: req.user.id, request_type: requestType, status, decision_reason_code: reasonCode, submitted_at: now, updated_at: now });
      memoryStore.data_subject_request_events.push({ id: newId(), request_id: id, actor_id: req.user.id, event: status === 'DEFERRED' ? 'DEFERRED' : 'SUBMITTED', reason_code: reasonCode || 'REQUEST_RECEIVED', created_at: now });
      memoryStore.data_subject_request_revisions.push({ request_id: id, revision: 1, request_type: requestType, actor_id: req.user.id, created_at: now });
    }
    return res.status(202).json({ received: true, request_id: requestId, status: 'under_review' });
  } catch (_) {
    return res.status(503).json({ error: 'The request could not be recorded at this time.' });
  }
});

router.get('/requests/:requestId', requireAuth, async (req, res) => {
  try {
    let row;
    if (isPostgresActive()) {
      const result = await query(`SELECT request_id,status,request_type,submitted_at FROM data_subject_requests WHERE request_id=$1 AND requester_id=$2`, [req.params.requestId, req.user.id]);
      row = result.rows[0];
    } else {
      row = memoryStore.data_subject_requests.find(item => item.request_id === req.params.requestId && item.requester_id === req.user.id);
    }
    if (!row) return res.status(404).json({ error: 'Request not found.' });
    return res.json({ request_id: row.request_id, status: row.status === 'PENDING' || row.status === 'IN_REVIEW' || row.status === 'DEFERRED' ? 'under_review' : 'updated', request_type: row.request_type, submitted_at: row.submitted_at });
  } catch (_) { return res.status(503).json({ error: 'Request status is temporarily unavailable.' }); }
});

router.post('/consent/:purposeKey', requireAuth, async (req, res) => {
  const decision = req.body?.decision;
  if (!['GRANTED', 'DECLINED', 'WITHDRAWN'].includes(decision)) return res.status(400).json({ error: 'A valid consent choice is required.' });
  try {
    const accepted = await withTransaction(client => recordConsentChoice(client, {
      userId: req.user.id, purposeKey: req.params.purposeKey, decision, channel: 'ANDROID', now: Date.now()
    }));
    if (!accepted) return res.status(404).json({ error: 'Consent purpose unavailable.' });
    return res.json({ success: true });
  } catch (_) { return res.status(503).json({ error: 'Consent choice is temporarily unavailable.' }); }
});

router.post('/public-policy/:version/publish', requireAuth, async (req, res) => {
  const { approval_reference: approvalReference, reason_code: reasonCode } = req.body || {};
  if (typeof reasonCode !== 'string' || !/^[A-Z0-9_]{2,64}$/.test(reasonCode)) {
    return res.status(400).json({ error: 'A reason code is required.' });
  }
  try {
    const privilegedToken = req.get('X-Privileged-Session') || '';
    const now = Date.now();
    await withTransaction(async client => {
      if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
      await requireAdminAuthority(client, { userId: req.user.id, authToken: req.token, privilegedToken, capability: 'privacy.document.publish', scope: 'global', lock: true });
      const selected = await client.query(`SELECT * FROM privacy_policy_versions WHERE version=$1 AND document_kind='PUBLIC_POLICY' FOR UPDATE`, [req.params.version]);
      const document = selected.rows[0];
      if (!document || document.publication_status !== 'DRAFT') throw new PhaseBError(409, 'PUBLICATION_UNAVAILABLE', 'Privacy document publication is unavailable');
      const documentPath = path.resolve(PROJECT_ROOT, document.document_path);
      const relative = path.relative(PUBLIC_POLICY_ROOT, documentPath);
      if (relative.startsWith('..') || path.isAbsolute(relative)) throw new PhaseBError(409, 'PUBLICATION_UNAVAILABLE', 'Privacy document publication is unavailable');
      const content = fs.readFileSync(documentPath, 'utf8');
      if (crypto.createHash('sha256').update(content, 'utf8').digest('hex') !== String(document.content_hash).trim()) {
        throw new PhaseBError(409, 'PUBLICATION_UNAVAILABLE', 'Privacy document publication is unavailable');
      }
      if (typeof approvalReference !== 'string' || !approvalReference.trim()) {
        throw new PhaseBError(400, 'APPROVAL_REFERENCE_REQUIRED', 'A supplied approval reference is required');
      }
      await client.query(`UPDATE privacy_policy_versions SET publication_status='SUPERSEDED',is_active=FALSE WHERE document_kind='PUBLIC_POLICY' AND publication_status='PUBLISHED' AND is_active=TRUE`);
      await client.query(`UPDATE privacy_policy_versions SET publication_status='PUBLISHED',effective_at=$2,is_active=TRUE,approval_reference=$3 WHERE version=$1`, [document.version, now, approvalReference.trim()]);
      await client.query(`INSERT INTO privacy_publication_events (id,document_version,event,actor_id,actor_role,decision_reference,reason_code,created_at) VALUES ($1,$2,'PUBLISHED',$3,$4,$5,$6,$7)`, [newId(), document.version, req.user.id, req.user.role || 'USER', approvalReference.trim(), reasonCode, now]);
    });
    return res.json({ success: true, version: req.params.version, effective_at: now });
  } catch (error) {
    if (error && error.code === 'PUBLICATION_UNAVAILABLE') return res.status(409).json({ error: 'Privacy document publication is unavailable.' });
    if (error instanceof PhaseBError) return res.status(error.status).json({ code: error.code, error: error.message });
    return res.status(503).json({ error: 'Privacy document publication is unavailable.' });
  }
});

router.post('/public-policy/:version/withdraw', requireAuth, async (req, res) => {
  const { reason_code: reasonCode, decision_reference: decisionReference } = req.body || {};
  if (typeof reasonCode !== 'string' || !/^[A-Z0-9_]{2,64}$/.test(reasonCode)) return res.status(400).json({ error: 'A reason code is required.' });
  try {
    const privilegedToken = req.get('X-Privileged-Session') || '';
    const now = Date.now();
    await withTransaction(async client => {
      if (!privilegedToken) throw new PhaseBError(401, 'STEP_UP_REQUIRED', 'Fresh administrator authentication is required');
      await requireAdminAuthority(client, { userId: req.user.id, authToken: req.token, privilegedToken, capability: 'privacy.document.publish', scope: 'global', lock: true });
      const selected = await client.query(`SELECT version FROM privacy_policy_versions WHERE version=$1 AND document_kind='PUBLIC_POLICY' AND publication_status='PUBLISHED' AND is_active=TRUE FOR UPDATE`, [req.params.version]);
      if (!selected.rows.length) throw new PhaseBError(409, 'PUBLICATION_UNAVAILABLE', 'Privacy document publication is unavailable');
      await client.query(`UPDATE privacy_policy_versions SET publication_status='WITHDRAWN',withdrawn_at=$2,is_active=FALSE WHERE version=$1`, [req.params.version, now]);
      await client.query(`INSERT INTO privacy_publication_events (id,document_version,event,actor_id,actor_role,decision_reference,reason_code,created_at) VALUES ($1,$2,'WITHDRAWN',$3,$4,$5,$6,$7)`, [newId(), req.params.version, req.user.id, req.user.role || 'USER', typeof decisionReference === 'string' ? decisionReference : null, reasonCode, now]);
    });
    return res.json({ success: true, version: req.params.version, withdrawn_at: now });
  } catch (error) {
    if (error instanceof PhaseBError) return res.status(error.status).json({ code: error.code, error: error.message });
    return res.status(503).json({ error: 'Privacy document publication is unavailable.' });
  }
});

module.exports = router;
