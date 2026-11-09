'use strict';

const { isPostgresActive } = require('../db');

async function hasCurrentConsent(client, userId, purposeKey) {
  if (!isPostgresActive()) return false;
  const result = await client.query(
    `SELECT p.purpose_key
       FROM consent_purposes p
       JOIN LATERAL (
         SELECT decision,wording_version,wording_hash
           FROM consent_events e
          WHERE e.user_id=$1 AND e.purpose_key=p.purpose_key
          ORDER BY created_at DESC,id DESC LIMIT 1
       ) current ON current.wording_version=p.wording_version AND current.wording_hash=p.wording_hash
      WHERE p.purpose_key=$2 AND p.active=TRUE AND current.decision='GRANTED'`,
    [userId, purposeKey]
  );
  return result.rows.length > 0;
}

async function recordConsentChoice(client, { userId, purposeKey, decision, channel = 'ANDROID', now = Date.now() }) {
  if (!['GRANTED', 'DECLINED', 'WITHDRAWN'].includes(decision)) throw new Error('Invalid consent decision');
  const purpose = await client.query(`SELECT purpose_key,wording_version,wording_hash FROM consent_purposes WHERE purpose_key=$1 AND active=TRUE`, [purposeKey]);
  if (!purpose.rows.length) return false;
  await client.query(
    `INSERT INTO consent_events (id,user_id,purpose_key,wording_version,wording_hash,decision,channel,created_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
    [require('crypto').randomUUID(), userId, purposeKey, purpose.rows[0].wording_version, purpose.rows[0].wording_hash, decision, channel, now]
  );
  return true;
}

function retentionDryRun({ fixtures, rules, holds, now = Date.now() }) {
  if (!Array.isArray(fixtures) || !Array.isArray(rules) || !Array.isArray(holds)) throw new TypeError('Synthetic fixtures, rules, and holds are required');
  if (fixtures.some(row => typeof row.subject_ref !== 'string' || !row.subject_ref.startsWith('synthetic:'))) throw new Error('Dry-run accepts synthetic fixtures only');
  const groups = new Map();
  for (const row of fixtures) {
    const rule = rules.find(item => item.rule_key === row.rule_key);
    const holdActive = holds.some(item => item.subject_ref === row.subject_ref && item.status === 'ACTIVE');
    let outcome = 'NOT_EXECUTABLE';
    let trigger = rule ? rule.trigger_definition : 'NO_RULE';
    if (holdActive) outcome = 'BLOCKED_HOLD';
    else if (!rule || rule.status !== 'APPROVED' || !Number.isFinite(rule.duration_days)) outcome = 'NOT_EXECUTABLE';
    else if (Number.isFinite(rule.review_due_at) && rule.review_due_at <= now) outcome = 'REVIEW_REQUIRED';
    else if (!Number.isFinite(row.trigger_at)) outcome = 'REVIEW_REQUIRED';
    else if (now - row.trigger_at >= rule.duration_days * 86400000) outcome = 'ELIGIBLE_PENDING_CONFIRMATION';
    else outcome = 'NOT_DUE';
    const key = [row.rule_key || 'NO_RULE', trigger, holdActive ? 'ACTIVE' : 'NONE', outcome].join('|');
    groups.set(key, (groups.get(key) || 0) + 1);
  }
  return [...groups].map(([key, count]) => {
    const [rule, trigger, hold_status, proposed_outcome] = key.split('|');
    return { rule, trigger, hold_status, proposed_outcome, count };
  }).sort((a, b) => `${a.rule}|${a.trigger}|${a.hold_status}|${a.proposed_outcome}`.localeCompare(`${b.rule}|${b.trigger}|${b.hold_status}|${b.proposed_outcome}`));
}

async function recordSyntheticDisposal(client, { rule, subjectRef, reviewDueAt, activeHold, confirmed, authorizedBy, confirmedBy, now = Date.now() }) {
  if (!subjectRef?.startsWith('synthetic:') || rule?.status !== 'APPROVED' || !Number.isFinite(rule.duration_days) ||
      !Number.isFinite(reviewDueAt) || reviewDueAt <= now || activeHold || confirmed !== true ||
      !authorizedBy || !confirmedBy || authorizedBy === confirmedBy) {
    throw new Error('Explicit current authorization and synthetic eligibility are required');
  }
  const crypto = require('crypto');
  const id = crypto.randomUUID();
  await client.query(
    `INSERT INTO disposal_events (id,rule_key,subject_ref,outcome,reason_code,authorized_by,confirmed_by,created_at)
     VALUES ($1,$2,$3,'DISPOSED','EXPLICIT_SYNTHETIC_CONFIRMATION',$4,$5,$6)`,
    [id, rule.rule_key, subjectRef, authorizedBy, confirmedBy, now]
  );
  return id;
}

module.exports = { hasCurrentConsent, recordConsentChoice, recordSyntheticDisposal, retentionDryRun };
