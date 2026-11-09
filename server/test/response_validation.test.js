'use strict';

const assert = require('node:assert/strict');
const V = require('../src/phase-b/responseValidation');
const { sha256 } = require('../src/phase-b/governedKnowledge');

const source = { sourceId: 'constitution-fixture', title: 'Synthetic constitution', version: 'v1',
  authorityClass: 'OFFICIAL_CONSTITUTION', approvalStatus: 'APPROVED', publicationStatus: 'PUBLISHED',
  location: 'Section 1', contentHash: sha256('Synthetic church position evidence.'),
  text: 'Synthetic church position evidence.' };
const scripture = { reference: 'Genesis 1:1', book: 'Genesis', chapter: 1, startVerse: 1, endVerse: 1,
  translationId: 'KJV', corpusId: 'synthetic-kjv',
  corpusVersion: 'fixture-v1', corpusHash: 'a'.repeat(64), text: 'In the beginning God created the heaven and the earth.',
  verses: [{ verse: 1, text: 'In the beginning God created the heaven and the earth.',
    contentHash: sha256('In the beginning God created the heaven and the earth.') }] };
const context = { passages: [source], scriptures: [scripture] };
function candidate(overrides = {}) {
  return { category: 'PASSAGE_INTERPRETATION', answer: 'The passage opens with “In the beginning”.',
    sourceReferences: [{ sourceId: source.sourceId, version: source.version, location: source.location }],
    scriptureReferences: [{ reference: scripture.reference, translationId: scripture.translationId,
      corpusId: scripture.corpusId, corpusVersion: scripture.corpusVersion, corpusHash: scripture.corpusHash,
    quote: 'In the beginning' }], confidence: 'HIGH', escalationRequired: false, ...overrides };
}

const valid = V.validateModelResponse(candidate(), context);
assert.equal(valid.valid, true);
assert.equal(valid.response.escalationRequired, false);
assert.equal(V.validateModelResponse(candidate({ escalationRequired: true }), context).code, 'MODEL_REQUESTED_ESCALATION', 'a model-requested escalation cannot be suppressed');
assert.equal(valid.response.confidence, 'LOW', 'model self-confidence is not carried forward');
assert.equal(valid.response.scriptureReferences[0].text, scripture.text, 'returned passage is the exact stored corpus text');
assert.equal(valid.response.scriptureReferences[0].book, 'Genesis');
assert.equal(valid.response.scriptureReferences[0].startVerse, 1);
assert.equal(V.validateModelResponse(candidate({ category: 'PASTORAL_ADVICE' }), context).code, 'OUT_OF_SCOPE');
assert.equal(V.validateModelResponse(candidate({ sourceReferences: [{ sourceId: 'fake', version: 'v1', location: 'Section 1' }] }), context).code, 'UNVERIFIED_SOURCE_REFERENCE');
assert.equal(V.validateModelResponse(candidate({ sourceReferences: [] }), context).code, 'SOURCE_PROVENANCE_REQUIRED');
assert.equal(V.validateModelResponse(candidate({ scriptureReferences: [{ ...candidate().scriptureReferences[0], corpusHash: 'c'.repeat(64) }] }), context).code, 'UNVERIFIED_SCRIPTURE_REFERENCE');
assert.equal(V.validateModelResponse(candidate({ scriptureReferences: [{ ...candidate().scriptureReferences[0], quote: 'made up quotation' }] }), context).code, 'UNVERIFIED_SCRIPTURE_REFERENCE');
assert.equal(V.validateModelResponse(candidate({ answer: 'The passage says “unsupported quotation”.' }), context).code, 'UNSUPPORTED_QUOTATION');
const noOfficial = { ...context, passages: [{ ...source, authorityClass: 'PERSONAL_THEOLOGY' }] };
assert.equal(V.validateModelResponse(candidate({ answer: 'The church officially teaches this rule.' }), noOfficial).code, 'UNSUPPORTED_INSTITUTIONAL_CLAIM');
const misleadingOfficial = { ...context, passages: [{ ...source,
  text: 'Synthetic policy: marriage and ministry receive review. This section says nothing about member eligibility.',
  contentHash: sha256('Synthetic policy: marriage and ministry receive review. This section says nothing about member eligibility.') }] };
assert.equal(V.validateModelResponse(candidate({ answer: 'The church officially requires marriage and ministry for member eligibility.' }), misleadingOfficial).code,
  'UNSUPPORTED_INSTITUTIONAL_CLAIM', 'lexical overlap cannot establish official policy support');
assert.equal(V.validateModelResponse(candidate({ answer: 'According to Perazim policy, this rule is required.' }), context).code,
  'UNSUPPORTED_INSTITUTIONAL_CLAIM', 'institutional claims framed as source attribution fail closed');
assert.equal(V.validateModelResponse(candidate({ answer: 'Perazim says this is the only permitted view.' }), context).code,
  'UNSUPPORTED_INSTITUTIONAL_CLAIM', 'institutional claims phrased as attribution fail closed');
assert.equal(V.validateModelResponse(candidate(), { ...context, passages: [{ ...source, text: 'Tampered evidence.' }] }).code,
  'UNVERIFIED_SOURCE_REFERENCE', 'a changed source passage cannot retain its old content hash');
assert.equal(V.validateModelResponse(candidate(), { ...context, scriptures: [{ ...scripture,
  verses: [{ ...scripture.verses[0], contentHash: 'c'.repeat(64) }] }] }).code,
  'UNVERIFIED_SCRIPTURE_REFERENCE', 'a corrupted verse hash fails closed');
assert.equal(V.validateModelResponse(candidate({ answer: 'You should stop taking your medication.' }), context).code, 'PROHIBITED_ADVICE');
assert.equal(V.validateModelResponse(candidate({ answer: 'In my pastoral experience, this is true.' }), context).code, 'FABRICATED_TESTIMONY');
assert.equal(V.validateModelResponse('{"answer":"bad"}', context).code, 'INVALID_RESPONSE');

process.stdout.write('PASS: structured answer allowlist, source and Scripture provenance, exact quotation checks, institutional support, advice limits, and fail-closed response.\n');
