'use strict';

const assert = require('node:assert/strict');
const S = require('../src/phase-b/scriptureCorpus');

function fixture() {
  const verseText = 'Synthetic Bible-like text for the loader fixture.';
  const bundle = {
    manifest: { schemaVersion: 1, translationId: 'SYN', sourceName: 'Synthetic fixture', sourceVersion: 'fixture-v1',
      licenseEvidenceReference: 'TEST-FIXTURE-ONLY', licenseStatus: 'TEST_FIXTURE', bookCount: 1,
      chapterCount: 1, verseCount: 1, integrityValidationResult: 'TEST_ONLY' },
    books: [{ bookNumber: 1, bookId: 'SYN1', name: 'Synthetic Book', aliases: ['Syn'], chapterCount: 1 }],
    verses: [{ translationId: 'SYN', bookNumber: 1, chapterNumber: 1, verseNumber: 1,
      text: verseText, contentHash: S.sha256(verseText) }]
  };
  bundle.manifest.corpusHash = S.sha256(S.canonicalCorpusPayload(bundle));
  return bundle;
}

async function main() {
  const source = fixture();
  assert.throws(() => S.validateCorpusBundle(source), { code: 'UNAUTHORIZED_CORPUS' }, 'fixtures cannot enter production import path');
  const validated = S.validateCorpusBundle(source, { allowSyntheticFixture: true });
  assert.equal(validated.verseCount, 1);
  assert.equal(validated.integrityValidationResult, 'TEST_ONLY');
  const undersizedAuthorized = fixture();
  undersizedAuthorized.manifest = { ...undersizedAuthorized.manifest, licenseStatus: 'AUTHORIZED', integrityValidationResult: 'VALIDATED' };
  assert.throws(() => S.validateCorpusBundle(undersizedAuthorized), { code: 'INCOMPLETE_CORPUS' }, 'a test fixture cannot masquerade as a complete authorized corpus');
  assert.equal(S.extractScriptureReferences('Read Syn 1:1 and Genesis 1:1-2').length, 1, 'only canonical supported references are extracted');
  assert.throws(() => S.validateCorpusBundle({ ...source, manifest: { ...source.manifest, verseCount: 2 } }, { allowSyntheticFixture: true }), { code: 'CORPUS_COUNT_MISMATCH' });
  const tampered = fixture();
  tampered.verses[0].text = 'changed after manifest preparation';
  assert.throws(() => S.validateCorpusBundle(tampered, { allowSyntheticFixture: true }), { code: 'INVALID_VERSE' });

  const inserts = [];
  const imported = await S.importCorpus({ async query(sql, params) { inserts.push({ sql, params }); return { rows: [] }; } }, source, {
    actorId: 'synthetic-importer', now: () => 12345, allowSyntheticFixture: true
  });
  assert.equal(imported.publicationStatus, 'STAGED');
  assert.equal(inserts[0].params[11], 12345, 'loader records the actual import time');
  assert.equal(inserts.length, 3, 'corpus, book and exact verse rows are written');
  assert.doesNotMatch(inserts[0].sql, /PUBLISHED/);

  const queryCalls = [];
  const lookup = await S.lookupExactScripture(async (sql, params) => {
    queryCalls.push(sql);
    if (/FROM ai_scripture_corpora/.test(sql)) return { rows: [{ corpus_id: 'SYN-v1', translation_id: 'SYN', source_version: 'v1', corpus_hash: 'b'.repeat(64), integrity_validation_result: 'VALIDATED', license_status: 'AUTHORIZED' }] };
    if (/FROM ai_scripture_books/.test(sql)) return { rows: [{ book_number: 1, book_id: 'GEN', name: 'Genesis', aliases: ['Gen'], chapter_count: 50 }] };
    return { rows: [{ verse_number: 1, text: 'Exact synthetic verse.', content_hash: S.sha256('Exact synthetic verse.') }] };
  }, 'Gen 1:1', 'KJV');
  assert.equal(lookup.ok, true);
  assert.equal(lookup.text, 'Exact synthetic verse.');
  assert.equal(lookup.corpusVersion, 'v1');
  assert.equal(lookup.corpusHash, 'b'.repeat(64));
  assert.match(queryCalls[0], /publication_status='PUBLISHED'/);
  assert.match(queryCalls[0], /license_status='AUTHORIZED'/);
  assert.equal((await S.lookupExactScripture(async () => ({ rows: [] }), 'Genesis 1:1', 'KJV')).code, 'CORPUS_UNAVAILABLE');
  assert.equal((await S.lookupExactScripture(async () => ({ rows: [] }), 'Genesis 0:1', 'KJV')).code, 'INVALID_REFERENCE');
  assert.equal((await S.lookupExactScripture(async () => ({ rows: [] }), 'Genesis 1:1', 'NOPE')).code, 'TRANSLATION_UNAVAILABLE');

  const publishCalls = [];
  const published = await S.publishCorpus({ async query(sql, params) {
    publishCalls.push({ sql, params });
    if (/SELECT \* FROM ai_scripture_corpora WHERE corpus_id/.test(sql)) return { rows: [{ corpus_id: 'SYN-v1', translation_id: 'SYN', imported_by: 'importer', publication_status: 'STAGED', license_status: 'AUTHORIZED', integrity_validation_result: 'VALIDATED', book_count: 1, chapter_count: 1, verse_count: 1 }] };
    if (/SELECT \(SELECT count\(\*\)/.test(sql)) return { rows: [{ book_count: 1, chapter_count: 1, verse_count: 1, present_chapters: 1, bad_book_count: false, bad_verse_sequence: false }] };
    if (/SELECT corpus_id,imported_by/.test(sql)) return { rows: [] };
    return { rows: [] };
  } }, { corpusId: 'SYN-v1', actorId: 'publisher', requestId: 'synthetic-request', now: () => 456 });
  assert.equal(published.publicationStatus, 'PUBLISHED');
  assert.equal(publishCalls.some(call => /FOR UPDATE/.test(call.sql)), true, 'publication serializes corpus state changes');

  process.stdout.write('PASS: synthetic corpus manifest, authorization guard, integrity validation, import timestamp, exact lookup provenance, and staged-only publication.\n');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
