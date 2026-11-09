'use strict';

const assert = require('assert/strict');
process.env.DATA01_IMPORT_FINGERPRINT_KEY = 'synthetic-test-key-only';
const { batchFingerprint, classifyImportRows, prepareImportRows } = require('../src/phase-b/membershipImport');

const complete = { reference: 'SYNTHETIC-001', full_name: 'Synthetic Member', phone: '0712345000' };
const stagedNameOnly = prepareImportRows([{ reference: 'SYNTHETIC-002', full_name: 'Synthetic Name Only' }]);
assert.equal(stagedNameOnly[0].rowStatus, 'INCOMPLETE');
assert.ok(stagedNameOnly[0].issues.includes('MISSING_PHONE'));

const malformedOptionalPhone = prepareImportRows([{ reference: 'SYNTHETIC-003', full_name: 'Synthetic Invalid Phone', phone: 'not-a-phone' }]);
assert.equal(malformedOptionalPhone[0].rowStatus, 'REJECTED');
assert.ok(malformedOptionalPhone[0].issues.includes('MALFORMED_PHONE'));

const shared = classifyImportRows(prepareImportRows([
  { reference: 'SYNTHETIC-004', full_name: 'Synthetic Household A', phone: '0712345001' },
  { reference: 'SYNTHETIC-005', full_name: 'Synthetic Household B', phone: '+254712345001' }
]));
assert.equal(shared.counts.shared_phone, 2);
assert.ok(shared.rows.every(row => row.rowStatus === 'REVIEW'));

const duplicateRefs = classifyImportRows(prepareImportRows([
  { reference: 'Synthetic-006', full_name: 'Synthetic Person A', phone: '0712345002' },
  { reference: 'synthetic-006', full_name: 'Synthetic Person B', phone: '0712345003' }
]));
assert.equal(duplicateRefs.counts.duplicate_reference, 2);
assert.equal(duplicateRefs.counts.identity_conflict, 2);

const duplicateIdentities = classifyImportRows(prepareImportRows([
  { reference: 'SYNTHETIC-007', full_name: 'Synthetic Possible Duplicate', phone: '0712345004' },
  { reference: 'SYNTHETIC-008', full_name: '  synthetic   possible duplicate ', phone: '+254712345004' }
]));
assert.equal(duplicateIdentities.counts.possible_duplicate, 2);

const existingConflict = classifyImportRows(prepareImportRows([complete]), {
  existingReferences: new Set(['synthetic-001']),
  existingPhones: new Set(['+254712345000']),
  existingIdentities: new Set(['synthetic member\u0000+254712345000']),
  existingReferenceIdentities: new Map([['synthetic-001', new Set(['different synthetic person\u0000+254712345000'])]])
});
assert.equal(existingConflict.rows[0].rowStatus, 'REVIEW');
assert.deepEqual(existingConflict.rows[0].issues.filter(issue => ['DUPLICATE_REFERENCE','SHARED_PHONE','POSSIBLE_DUPLICATE_PERSON','IDENTITY_CONFLICT'].includes(issue)).sort(),
  ['DUPLICATE_REFERENCE','IDENTITY_CONFLICT','POSSIBLE_DUPLICATE_PERSON','SHARED_PHONE']);

const accepted = classifyImportRows(prepareImportRows([complete]));
assert.equal(accepted.counts.accepted, 1);
assert.equal(batchFingerprint('SYNTHETIC_BATCH', accepted.rows), batchFingerprint('SYNTHETIC_BATCH', accepted.rows));
assert.notEqual(batchFingerprint('SYNTHETIC_BATCH', accepted.rows), batchFingerprint('SYNTHETIC_BATCH_CHANGED', accepted.rows));
const changedReplay = classifyImportRows(prepareImportRows([complete]), { changedBatchReplay: true });
assert.equal(changedReplay.counts.accepted, 0, 'changed replay never creates a second operational membership');
assert.equal(changedReplay.counts.changed_batch_replay, 1);
assert.equal(changedReplay.rows[0].rowStatus, 'REVIEW');
assert.ok(changedReplay.rows[0].issues.includes('CHANGED_BATCH_REPLAY'));

console.log('Membership import classification passed: missing and malformed phones, shared phones, duplicate references, possible duplicates, existing conflicts, and stable synthetic batch fingerprints.');
