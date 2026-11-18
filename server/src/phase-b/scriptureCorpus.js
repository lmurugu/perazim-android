'use strict';

const crypto = require('node:crypto');
const { lookupScripture, parseScriptureReference } = require('./governedKnowledge');

const MAX_VERSES_PER_PASSAGE = 50;
const MAX_SCRIPTURE_RESULTS = 3;
const MAX_IMPORT_VERSES = 40000;
const TRANSLATION_ID = /^[A-Z0-9][A-Z0-9_-]{1,15}$/;
const SUPPORTED_TRANSLATIONS = new Set(['KJV', 'SUV', 'NIV', 'NKJV', 'ESV']);
const STOP_WORDS = new Set(['about','after','again','also','and','are','because','been','being','but','can','could','does','doing','for','from','had','has','have','how','into','its','just','like','more','most','not','our','out','over','please','says','should','some','tell','than','that','the','their','them','then','there','these','they','this','those','through','what','when','where','which','who','will','with','would','you','your','bible','verse','verses','quote','read','explain']);
const BOOKS = [
  ['Genesis',['Gen']],['Exodus',['Exod','Ex']],['Leviticus',['Lev']],['Numbers',['Num']],['Deuteronomy',['Deut']],['Joshua',['Josh']],['Judges',['Judg']],['Ruth',['Ruth']],['1 Samuel',['1 Sam','I Samuel']],['2 Samuel',['2 Sam','II Samuel']],['1 Kings',['1 Kgs','I Kings']],['2 Kings',['2 Kgs','II Kings']],['1 Chronicles',['1 Chr','I Chronicles']],['2 Chronicles',['2 Chr','II Chronicles']],['Ezra',['Ezra']],['Nehemiah',['Neh']],['Esther',['Est']],['Job',['Job']],['Psalms',['Psalm','Ps']],['Proverbs',['Prov','Pr']],['Ecclesiastes',['Eccl']],['Song of Solomon',['Song of Songs','Song']],['Isaiah',['Isa']],['Jeremiah',['Jer']],['Lamentations',['Lam']],['Ezekiel',['Ezek']],['Daniel',['Dan']],['Hosea',['Hos']],['Joel',['Joel']],['Amos',['Amos']],['Obadiah',['Obad']],['Jonah',['Jon']],['Micah',['Mic']],['Nahum',['Nah']],['Habakkuk',['Hab']],['Zephaniah',['Zeph']],['Haggai',['Hag']],['Zechariah',['Zech']],['Malachi',['Mal']],['Matthew',['Matt','Mt']],['Mark',['Mk']],['Luke',['Lk']],['John',['Jn']],['Acts',['Ac']],['Romans',['Rom']],['1 Corinthians',['1 Cor','I Corinthians']],['2 Corinthians',['2 Cor','II Corinthians']],['Galatians',['Gal']],['Ephesians',['Eph']],['Philippians',['Phil']],['Colossians',['Col']],['1 Thessalonians',['1 Thess','I Thessalonians']],['2 Thessalonians',['2 Thess','II Thessalonians']],['1 Timothy',['1 Tim','I Timothy']],['2 Timothy',['2 Tim','II Timothy']],['Titus',['Tit']],['Philemon',['Philem']],['Hebrews',['Heb']],['James',['Jas']],['1 Peter',['1 Pet','I Peter']],['2 Peter',['2 Pet','II Peter']],['1 John',['1 Jn','I John']],['2 John',['2 Jn','II John']],['3 John',['3 Jn','III John']],['Jude',['Jude']],['Revelation',['Rev']]
];
const CANONICAL_CHAPTER_COUNTS = [50,40,27,36,34,24,21,4,31,24,22,25,29,36,10,13,10,42,150,31,12,8,66,52,5,48,12,14,3,9,1,4,7,3,3,3,2,14,4,28,16,24,21,28,16,16,13,6,6,4,4,5,3,6,4,3,1,13,5,5,3,5,1,1,1,22];
const BOOK_PATTERN = [...new Set(BOOKS.flatMap(([name, aliases]) => [name, ...aliases]))]
  .sort((a, b) => b.length - a.length).map(escapeRegex).join('|');
const SCRIPTURE_REFERENCE_PATTERN = new RegExp(`\\b(${BOOK_PATTERN})\\s+(\\d{1,3}):(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?\\b`, 'gi');

class CorpusError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

function escapeRegex(value) { return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
function sha256(value) { return crypto.createHash('sha256').update(value, 'utf8').digest('hex'); }

function canonicalCorpusPayload(bundle) {
  const translationId = String(bundle.manifest.translationId || '').toUpperCase();
  const books = [...bundle.books].map(book => ({
    bookNumber: Number(book.bookNumber), bookId: String(book.bookId), name: String(book.name),
    aliases: [...(book.aliases || [])].map(String).sort(), chapterCount: Number(book.chapterCount)
  })).sort((a, b) => a.bookNumber - b.bookNumber);
  const verses = [...bundle.verses].map(verse => ({
    translationId: String(verse.translationId).toUpperCase(), bookNumber: Number(verse.bookNumber),
    chapterNumber: Number(verse.chapterNumber), verseNumber: Number(verse.verseNumber), text: String(verse.text)
  })).sort((a, b) => a.bookNumber - b.bookNumber || a.chapterNumber - b.chapterNumber || a.verseNumber - b.verseNumber);
  return JSON.stringify({ translationId, books, verses });
}

function validateCorpusBundle(bundle, { allowSyntheticFixture = false } = {}) {
  if (!bundle || !bundle.manifest || !Array.isArray(bundle.books) || !Array.isArray(bundle.verses)) {
    throw new CorpusError('INVALID_CORPUS', 'A manifest, book list and verse list are required.');
  }
  const m = bundle.manifest;
  const translationId = String(m.translationId || '').toUpperCase();
  if (!TRANSLATION_ID.test(translationId) || typeof m.sourceName !== 'string' || !m.sourceName.trim() || m.sourceName.length > 200 ||
      typeof m.sourceVersion !== 'string' || !m.sourceVersion.trim() || m.sourceVersion.length > 100 ||
      typeof m.licenseEvidenceReference !== 'string' || !m.licenseEvidenceReference.trim() || m.licenseEvidenceReference.length > 400) {
    throw new CorpusError('MISSING_CORPUS_PROVENANCE', 'Translation, source, version and licence evidence reference are required.');
  }
  const fixture = m.licenseStatus === 'TEST_FIXTURE';
  if (fixture && !allowSyntheticFixture) throw new CorpusError('UNAUTHORIZED_CORPUS', 'Synthetic test corpora cannot be imported as authorized Scripture.');
  if (!fixture && m.licenseStatus !== 'AUTHORIZED') throw new CorpusError('UNAUTHORIZED_CORPUS', 'A verified authorization reference is required.');
  if (fixture && m.integrityValidationResult && m.integrityValidationResult !== 'TEST_ONLY') throw new CorpusError('INVALID_MANIFEST', 'Synthetic corpus integrity status is generated by the loader.');
  if (!Number.isInteger(m.schemaVersion) || m.schemaVersion !== 1) throw new CorpusError('INVALID_MANIFEST', 'Unsupported corpus manifest version.');
  if (!/^[a-f0-9]{64}$/.test(m.corpusHash || '')) throw new CorpusError('INVALID_MANIFEST', 'A corpus SHA-256 is required.');
  if (![m.bookCount, m.chapterCount, m.verseCount].every(Number.isSafeInteger)) throw new CorpusError('INVALID_MANIFEST', 'Corpus counts must be integers.');
  if (bundle.verses.length > MAX_IMPORT_VERSES || !bundle.books.length || !bundle.verses.length) throw new CorpusError('INVALID_CORPUS', 'Corpus is empty or exceeds the import limit.');
  if (m.bookCount !== bundle.books.length || m.verseCount !== bundle.verses.length) throw new CorpusError('CORPUS_COUNT_MISMATCH', 'Manifest counts do not match corpus contents.');
  if (!fixture && (bundle.books.length !== 66 || m.chapterCount !== 1189)) {
    throw new CorpusError('INCOMPLETE_CORPUS', 'A publishable Bible corpus must contain all 66 books and 1,189 chapters.');
  }

  const seenBooks = new Set();
  const seenBookIds = new Set();
  let chapterCount = 0;
  for (const book of bundle.books) {
    if (!Number.isInteger(book.bookNumber) || book.bookNumber < 1 || book.bookNumber > 66 || seenBooks.has(book.bookNumber) ||
        typeof book.bookId !== 'string' || !book.bookId.trim() || typeof book.name !== 'string' || !book.name.trim() ||
        !Number.isInteger(book.chapterCount) || book.chapterCount < 1 || !Array.isArray(book.aliases || []) ||
        (book.aliases || []).some(alias => typeof alias !== 'string' || !alias.trim() || alias.length > 80)) {
      throw new CorpusError('INVALID_BOOK', 'Book metadata is malformed or duplicated.');
    }
    if (seenBookIds.has(book.bookId)) throw new CorpusError('INVALID_BOOK', 'Book identifiers must be unique within a corpus.');
    seenBooks.add(book.bookNumber);
    seenBookIds.add(book.bookId);
    chapterCount += book.chapterCount;
  }
  if (!fixture && [...seenBooks].some((bookNumber, index) => bookNumber !== index + 1)) {
    throw new CorpusError('INCOMPLETE_CORPUS', 'All 66 books must use a contiguous canonical book order.');
  }
  if (!fixture && bundle.books.some(book => book.name !== BOOKS[book.bookNumber - 1][0] || book.chapterCount !== CANONICAL_CHAPTER_COUNTS[book.bookNumber - 1])) {
    throw new CorpusError('INVALID_BOOK', 'Book names and chapter counts must match the canonical 66-book order.');
  }
  if (chapterCount !== m.chapterCount) throw new CorpusError('CORPUS_COUNT_MISMATCH', 'Manifest chapter count does not match book metadata.');

  const booksByNumber = new Map(bundle.books.map(book => [book.bookNumber, book]));
  const versesByChapter = new Map();
  const seenVerses = new Set();
  for (const verse of bundle.verses) {
    const text = verse && verse.text;
    const book = verse && booksByNumber.get(verse.bookNumber);
    if (!book || String(verse.translationId || '').toUpperCase() !== translationId || !Number.isInteger(verse.chapterNumber) || verse.chapterNumber < 1 || verse.chapterNumber > book.chapterCount ||
        !Number.isInteger(verse.verseNumber) || verse.verseNumber < 1 || typeof text !== 'string' || !text.trim() || text.length > 8000 ||
        !/^[a-f0-9]{64}$/.test(verse.contentHash || '') || sha256(text) !== verse.contentHash) {
      throw new CorpusError('INVALID_VERSE', 'Verse metadata, translation or content hash is invalid.');
    }
    const key = `${verse.bookNumber}:${verse.chapterNumber}:${verse.verseNumber}`;
    if (seenVerses.has(key)) throw new CorpusError('DUPLICATE_VERSE', 'Corpus contains a duplicate verse.');
    seenVerses.add(key);
    const chapterKey = `${verse.bookNumber}:${verse.chapterNumber}`;
    if (!versesByChapter.has(chapterKey)) versesByChapter.set(chapterKey, []);
    versesByChapter.get(chapterKey).push(verse.verseNumber);
  }
  for (const book of bundle.books) {
    for (let chapter = 1; chapter <= book.chapterCount; chapter++) {
      const numbers = (versesByChapter.get(`${book.bookNumber}:${chapter}`) || []).sort((a, b) => a - b);
      if (!numbers.length || numbers.some((number, index) => number !== index + 1)) {
        throw new CorpusError('INCOMPLETE_CHAPTER', 'Each chapter must contain a contiguous verse sequence starting at 1.');
      }
    }
  }
  const actualHash = sha256(canonicalCorpusPayload(bundle));
  if (actualHash !== m.corpusHash) throw new CorpusError('CORPUS_HASH_MISMATCH', 'The corpus hash does not match its canonical contents.');
  return Object.freeze({
    corpusId: `${translationId}-${String(m.sourceVersion).replace(/[^A-Za-z0-9._-]/g, '_')}-${actualHash.slice(0, 16)}`,
    translationId, sourceName: String(m.sourceName), sourceVersion: String(m.sourceVersion),
    licenseEvidenceReference: String(m.licenseEvidenceReference), licenseStatus: fixture ? 'TEST_FIXTURE' : 'AUTHORIZED',
    corpusHash: actualHash, bookCount: bundle.books.length, chapterCount, verseCount: bundle.verses.length,
    integrityValidationResult: fixture ? 'TEST_ONLY' : 'VALIDATED', importedAt: null,
    books: bundle.books, verses: bundle.verses, manifest: { ...m, corpusHash: actualHash }
  });
}

function extractScriptureReferences(question) {
  if (typeof question !== 'string') return [];
  const results = [];
  const seen = new Set();
  SCRIPTURE_REFERENCE_PATTERN.lastIndex = 0;
  for (const match of question.matchAll(SCRIPTURE_REFERENCE_PATTERN)) {
    const candidate = `${match[1]} ${match[2]}:${match[3]}${match[4] ? `-${match[4]}` : ''}`;
    const parsed = parseScriptureReference(candidate);
    if (!parsed || parsed.endVerse - parsed.startVerse + 1 > MAX_VERSES_PER_PASSAGE) continue;
    const key = `${parsed.book.toLowerCase()}:${parsed.chapter}:${parsed.startVerse}:${parsed.endVerse}`;
    if (!seen.has(key)) { seen.add(key); results.push({ reference: candidate, parsed }); }
    if (results.length >= MAX_SCRIPTURE_RESULTS) break;
  }
  return results;
}

async function importCorpus(client, bundle, { actorId, now = () => Date.now(), allowSyntheticFixture = false } = {}) {
  if (!client || typeof client.query !== 'function') throw new CorpusError('IMPORT_STORE_REQUIRED', 'A database transaction is required.');
  if (!actorId) throw new CorpusError('IMPORT_ACTOR_REQUIRED', 'An authenticated importer is required.');
  const corpus = validateCorpusBundle(bundle, { allowSyntheticFixture });
  const importedAt = now();
  await client.query(
    `INSERT INTO ai_scripture_corpora
      (corpus_id,translation_id,source_name,source_version,license_evidence_reference,license_status,
       corpus_hash,book_count,chapter_count,verse_count,integrity_validation_result,imported_at,imported_by,
       publication_status,manifest)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,'STAGED',$14::jsonb)`,
    [corpus.corpusId, corpus.translationId, corpus.sourceName, corpus.sourceVersion,
      corpus.licenseEvidenceReference, corpus.licenseStatus, corpus.corpusHash, corpus.bookCount,
      corpus.chapterCount, corpus.verseCount, corpus.integrityValidationResult, importedAt,
      actorId, JSON.stringify({ ...corpus.manifest, integrityValidationResult: corpus.integrityValidationResult, importedAt })]
  );
  for (const book of corpus.books) {
    const bookVerses = corpus.verses.filter(verse => verse.bookNumber === book.bookNumber);
    await client.query(
      `INSERT INTO ai_scripture_books (corpus_id,book_number,book_id,name,aliases,chapter_count,verse_count)
       VALUES ($1,$2,$3,$4,$5::jsonb,$6,$7)`,
      [corpus.corpusId, book.bookNumber, book.bookId, book.name, JSON.stringify(book.aliases || []), book.chapterCount, bookVerses.length]
    );
  }
  const batchSize = 400;
  for (let offset = 0; offset < corpus.verses.length; offset += batchSize) {
    const batch = corpus.verses.slice(offset, offset + batchSize);
    const values = [];
    const params = [];
    for (const verse of batch) {
      const start = params.length;
      params.push(corpus.corpusId, verse.bookNumber, verse.chapterNumber, verse.verseNumber, verse.text, verse.contentHash);
      values.push(`($${start + 1},$${start + 2},$${start + 3},$${start + 4},$${start + 5},$${start + 6})`);
    }
    await client.query(
      `INSERT INTO ai_scripture_verses (corpus_id,book_number,chapter_number,verse_number,text,content_hash) VALUES ${values.join(',')}`,
      params
    );
  }
  return { corpusId: corpus.corpusId, translationId: corpus.translationId, corpusHash: corpus.corpusHash, bookCount: corpus.bookCount, chapterCount: corpus.chapterCount, verseCount: corpus.verseCount, integrityValidationResult: corpus.integrityValidationResult, publicationStatus: 'STAGED' };
}

async function lookupExactScripture(queryFn, reference, translationId) {
  const parsed = parseScriptureReference(reference);
  if (!parsed) return { ok: false, code: 'INVALID_REFERENCE' };
  if (typeof queryFn !== 'function') return { ok: false, code: 'CORPUS_UNAVAILABLE' };
  const translation = String(translationId || '').toUpperCase();
  if (!TRANSLATION_ID.test(translation)) return { ok: false, code: 'TRANSLATION_UNAVAILABLE' };
  if (!SUPPORTED_TRANSLATIONS.has(translation)) return { ok: false, code: 'TRANSLATION_UNAVAILABLE' };
  const corpora = await queryFn(
    `SELECT corpus_id,translation_id,source_version,corpus_hash,integrity_validation_result,license_status
       FROM ai_scripture_corpora WHERE translation_id=$1 AND publication_status='PUBLISHED'
         AND license_status='AUTHORIZED' AND integrity_validation_result='VALIDATED'`, [translation]
  );
  if (!corpora.rows.length) return { ok: false, code: 'CORPUS_UNAVAILABLE' };
  if (corpora.rows.length !== 1) return { ok: false, code: 'AMBIGUOUS_CORPUS' };
  const corpus = corpora.rows[0];
  const books = await queryFn(
    `SELECT book_number,book_id,name,aliases,chapter_count FROM ai_scripture_books WHERE corpus_id=$1 ORDER BY book_number`, [corpus.corpus_id]
  );
  const foundBook = books.rows.find(book => String(book.name).toLowerCase() === parsed.book.toLowerCase() ||
    (Array.isArray(book.aliases) && book.aliases.some(alias => String(alias).toLowerCase() === parsed.book.toLowerCase())));
  if (!foundBook || parsed.chapter > Number(foundBook.chapter_count)) return { ok: false, code: 'INVALID_REFERENCE' };
  if (parsed.endVerse - parsed.startVerse + 1 > MAX_VERSES_PER_PASSAGE) return { ok: false, code: 'INVALID_REFERENCE' };
  const result = await queryFn(
    `SELECT verse_number,text,content_hash FROM ai_scripture_verses
      WHERE corpus_id=$1 AND book_number=$2 AND chapter_number=$3 AND verse_number BETWEEN $4 AND $5
      ORDER BY verse_number`, [corpus.corpus_id, foundBook.book_number, parsed.chapter, parsed.startVerse, parsed.endVerse]
  );
  if (result.rows.length !== parsed.endVerse - parsed.startVerse + 1) return { ok: false, code: 'INVALID_REFERENCE' };
  const verses = result.rows.map(row => ({ verse: Number(row.verse_number), text: row.text, contentHash: row.content_hash }));
  if (verses.some((verse, index) => verse.verse !== parsed.startVerse + index || sha256(verse.text) !== verse.contentHash)) {
    return { ok: false, code: 'CORPUS_INTEGRITY_FAILURE' };
  }
  const verseRange = `${parsed.startVerse}${parsed.endVerse === parsed.startVerse ? '' : `-${parsed.endVerse}`}`;
  const canonicalReference = `${foundBook.name} ${parsed.chapter}:${verseRange}`;
  return {
    ok: true, reference: canonicalReference, book: foundBook.name, chapter: parsed.chapter,
    startVerse: parsed.startVerse, endVerse: parsed.endVerse, translationId: translation,
    corpusId: corpus.corpus_id, corpusVersion: corpus.source_version, corpusHash: corpus.corpus_hash,
    text: verses.map(verse => verse.text).join(' '), verses
  };
}

async function retrieveScriptureContext(queryFn, question, translationId) {
  const refs = extractScriptureReferences(question);
  if (refs.length) {
    const passages = [];
    for (const ref of refs) {
      const result = await lookupExactScripture(queryFn, ref.reference, translationId);
      if (!result.ok) return { sufficient: false, code: result.code, passages: [] };
      passages.push(result);
    }
    return { sufficient: true, code: null, passages };
  }
  return { sufficient: false, code: 'NO_VERIFIED_SCRIPTURE_MATCH', passages: [] };
}

async function publishCorpus(client, { corpusId, actorId, requestId, now = () => Date.now() }) {
  const result = await client.query('SELECT * FROM ai_scripture_corpora WHERE corpus_id=$1 FOR UPDATE', [corpusId]);
  const corpus = result.rows[0];
  if (!corpus) throw new CorpusError('CORPUS_NOT_FOUND', 'Scripture corpus was not found.');
  if (corpus.publication_status !== 'STAGED' || corpus.license_status !== 'AUTHORIZED' || corpus.integrity_validation_result !== 'VALIDATED') {
    throw new CorpusError('CORPUS_NOT_PUBLISHABLE', 'Only a staged corpus with verified authorization and integrity can be published.');
  }
  if (corpus.imported_by === actorId) throw new CorpusError('SEPARATION_OF_DUTIES', 'The corpus importer cannot publish the same corpus.');
  const integrity = await client.query(
    `SELECT (SELECT count(*) FROM ai_scripture_books WHERE corpus_id=$1)::int AS book_count,
            (SELECT coalesce(sum(chapter_count),0) FROM ai_scripture_books WHERE corpus_id=$1)::int AS chapter_count,
            (SELECT count(*) FROM ai_scripture_verses WHERE corpus_id=$1)::int AS verse_count,
            (SELECT count(DISTINCT (book_number,chapter_number)) FROM ai_scripture_verses WHERE corpus_id=$1)::int AS present_chapters,
            EXISTS (SELECT 1 FROM ai_scripture_books b WHERE b.corpus_id=$1 AND
              b.verse_count <> (SELECT count(*) FROM ai_scripture_verses v WHERE v.corpus_id=b.corpus_id AND v.book_number=b.book_number)) AS bad_book_count,
            EXISTS (SELECT 1 FROM ai_scripture_books b CROSS JOIN LATERAL generate_series(1,b.chapter_count) AS g(chapter_number)
              LEFT JOIN ai_scripture_verses v ON v.corpus_id=b.corpus_id AND v.book_number=b.book_number AND v.chapter_number=g.chapter_number
              WHERE b.corpus_id=$1 GROUP BY b.book_number,g.chapter_number
              HAVING count(v.verse_number)=0 OR min(v.verse_number) <> 1 OR max(v.verse_number) <> count(v.verse_number)) AS bad_verse_sequence`, [corpusId]
  );
  const checked = integrity.rows[0];
  if (!checked || Number(checked.book_count) !== Number(corpus.book_count) ||
      Number(checked.chapter_count) !== Number(corpus.chapter_count) ||
      Number(checked.verse_count) !== Number(corpus.verse_count) ||
      Number(checked.present_chapters) !== Number(corpus.chapter_count) || checked.bad_book_count || checked.bad_verse_sequence) {
    throw new CorpusError('CORPUS_INTEGRITY_FAILURE', 'Stored corpus records do not match the validated manifest.');
  }
  const current = await client.query(
    `SELECT corpus_id,imported_by FROM ai_scripture_corpora WHERE translation_id=$1 AND publication_status='PUBLISHED' FOR UPDATE`, [corpus.translation_id]
  );
  if (current.rows.some(row => row.imported_by === actorId)) throw new CorpusError('SEPARATION_OF_DUTIES', 'A corpus importer cannot replace its own published translation.');
  const publishedAt = now();
  await client.query(
    `UPDATE ai_scripture_corpora SET publication_status='RETIRED' WHERE translation_id=$1 AND publication_status='PUBLISHED'`, [corpus.translation_id]
  );
  await client.query(
    `UPDATE ai_scripture_corpora SET publication_status='PUBLISHED',published_at=$2,published_by=$3 WHERE corpus_id=$1 AND publication_status='STAGED'`,
    [corpusId, publishedAt, actorId]
  );
  return { corpusId, translationId: corpus.translation_id, publicationStatus: 'PUBLISHED',
    retiredCorpusIds: current.rows.map(row => row.corpus_id), publishedAt };
}

module.exports = {
  MAX_VERSES_PER_PASSAGE, MAX_SCRIPTURE_RESULTS, MAX_IMPORT_VERSES, SUPPORTED_TRANSLATIONS, CANONICAL_CHAPTER_COUNTS,
  CorpusError, sha256, canonicalCorpusPayload, validateCorpusBundle,
  extractScriptureReferences, importCorpus, lookupExactScripture,
  retrieveScriptureContext, publishCorpus
};
