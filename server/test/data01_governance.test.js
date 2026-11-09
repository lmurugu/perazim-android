'use strict';

const assert = require('node:assert/strict');
const { memoryStore } = require('../src/db');
const { retentionDryRun } = require('../src/phase-b/data01Governance');

const fixtureCountsBefore = [memoryStore.data_subject_requests.length, memoryStore.data_subject_request_events.length, memoryStore.privacy_notice_acknowledgements.length];
const now = 2_000_000_000_000;
const fixtures = [
  { subject_ref: 'synthetic:approved-expired', rule_key: 'SYNTHETIC_APPROVED', trigger_at: now - 10 * 86400000 },
  { subject_ref: 'synthetic:held', rule_key: 'SYNTHETIC_APPROVED', trigger_at: now - 10 * 86400000 },
  { subject_ref: 'synthetic:draft', rule_key: 'SYNTHETIC_DRAFT', trigger_at: now - 10 * 86400000 },
  { subject_ref: 'synthetic:review-expired', rule_key: 'SYNTHETIC_REVIEW_EXPIRED', trigger_at: now - 10 * 86400000 }
];
const rules = [
  { rule_key: 'SYNTHETIC_APPROVED', trigger_definition: 'SYNTHETIC_TRIGGER', status: 'APPROVED', duration_days: 3, review_due_at: now + 1000 },
  { rule_key: 'SYNTHETIC_DRAFT', trigger_definition: 'SYNTHETIC_TRIGGER', status: 'DRAFT', duration_days: null, review_due_at: null },
  { rule_key: 'SYNTHETIC_REVIEW_EXPIRED', trigger_definition: 'SYNTHETIC_TRIGGER', status: 'APPROVED', duration_days: 3, review_due_at: now - 1 }
];
const holds = [{ subject_ref: 'synthetic:held', status: 'ACTIVE' }];
const before = JSON.stringify({ fixtures, rules, holds });
const dryRun = retentionDryRun({ fixtures, rules, holds, now });
assert.deepEqual(dryRun.reduce((sum, row) => sum + row.count, 0), fixtures.length);
assert.ok(dryRun.some(row => row.proposed_outcome === 'ELIGIBLE_PENDING_CONFIRMATION'));
assert.ok(dryRun.some(row => row.proposed_outcome === 'BLOCKED_HOLD'));
assert.ok(dryRun.some(row => row.proposed_outcome === 'NOT_EXECUTABLE'));
assert.ok(dryRun.some(row => row.proposed_outcome === 'REVIEW_REQUIRED'));
assert.equal(JSON.stringify({ fixtures, rules, holds }), before, 'dry run leaves its synthetic inputs unchanged');
assert.deepEqual([memoryStore.data_subject_requests.length, memoryStore.data_subject_request_events.length, memoryStore.privacy_notice_acknowledgements.length], fixtureCountsBefore, 'dry run writes no records');
assert.throws(() => retentionDryRun({ fixtures: [{ subject_ref: 'member:real', rule_key: 'x' }], rules, holds, now }), /synthetic fixtures only/);

console.log('DATA-01 governance dry-run passed: draft, expiry review, legal hold, approved eligibility, and no writes.');
