'use strict';

const crypto = require('node:crypto');

const SOURCE_STATUSES = Object.freeze([
  'APPROVED', 'DRAFT', 'PENDING_APPROVAL', 'PROPOSED', 'WITHDRAWN',
  'SUPERSEDED', 'UNVERIFIED', 'RESTRICTED'
]);
const SENSITIVE_CLASSIFICATIONS = new Set([
  'MEMBERSHIP_REGISTER', 'PERSONAL_PROFILE', 'PASTORAL_RECORD',
  'PRAYER_REQUEST', 'DISCIPLINARY_RECORD', 'CREDENTIAL',
  'ADMIN_CORRESPONDENCE', 'RESTRICTED_CHURCH_RECORD'
]);
const CONFIDENTIALITY_LEVELS = new Set(['PUBLIC', 'RESTRICTED', 'CONFIDENTIAL', 'SENSITIVE']);
const INSTITUTIONAL_PURPOSE = 'INSTITUTIONAL';
const MAX_SOURCE_TEXT = 2_000_000;
const MAX_CHUNK_CHARS = 1200;
const CHUNK_OVERLAP_PARAGRAPHS = 1;
const MAX_CONTEXT_PASSAGES = 6;
const MAX_CONTEXT_CHARS = 9000;
const MAX_RETRIEVAL_CANDIDATES = 5000;

class KnowledgeError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function validateSource(source) {
  if (!source || typeof source !== 'object' || Array.isArray(source)) {
    throw new KnowledgeError('INVALID_SOURCE', 'Source metadata is required.');
  }
  const required = ['sourceId', 'title', 'documentType', 'authorityClass', 'status', 'confidentiality', 'confidentialityReviewer', 'confidentialityReviewedAt', 'permittedAudiences', 'permittedUses', 'sourceLocation', 'contentHash', 'language', 'version'];
  for (const field of required) {
    if (source[field] === undefined || source[field] === null || source[field] === '') {
      throw new KnowledgeError('MISSING_PROVENANCE', `Source ${field} is required.`);
    }
  }
  for (const field of ['sourceId', 'title', 'documentType', 'authorityClass', 'confidentialityReviewer', 'version', 'sourceLocation', 'language']) {
    if (typeof source[field] !== 'string' || !source[field].trim()) throw new KnowledgeError('INVALID_SOURCE', `Source ${field} must be text.`);
  }
  if (!CONFIDENTIALITY_LEVELS.has(source.confidentiality)) throw new KnowledgeError('INVALID_CLASSIFICATION', 'Confidentiality classification must be explicit.');
  if (!Number.isSafeInteger(source.confidentialityReviewedAt) || source.confidentialityReviewedAt < 1) {
    throw new KnowledgeError('CLASSIFICATION_REVIEW_REQUIRED', 'Confidentiality classification must have a review timestamp.');
  }
  if (!SOURCE_STATUSES.includes(source.status)) throw new KnowledgeError('INVALID_STATUS', 'Unsupported source status.');
  if (!/^[a-f0-9]{64}$/.test(source.contentHash)) throw new KnowledgeError('INVALID_HASH', 'A SHA-256 content hash is required.');
  if (!Array.isArray(source.permittedAudiences) || !source.permittedAudiences.length || source.permittedAudiences.some(x => typeof x !== 'string' || !x.trim()) ||
      !Array.isArray(source.permittedUses) || !source.permittedUses.length || source.permittedUses.some(x => typeof x !== 'string' || !x.trim())) {
    throw new KnowledgeError('MISSING_PROVENANCE', 'Audience and use permissions must be explicit lists.');
  }
  if (source.status === 'APPROVED' && (!source.approvalAuthority || !source.approvalReference || !source.approvedAt)) {
    throw new KnowledgeError('APPROVAL_EVIDENCE_REQUIRED', 'Approval status requires explicit authority, reference and timestamp.');
  }
  if (source.status === 'APPROVED' && (!Number.isSafeInteger(source.approvedAt) || typeof source.approvalAuthority !== 'string' || typeof source.approvalReference !== 'string')) {
    throw new KnowledgeError('APPROVAL_EVIDENCE_REQUIRED', 'Approval authority, reference and timestamp must be valid.');
  }
  if (source.status === 'WITHDRAWN' && !source.withdrawalReason) throw new KnowledgeError('WITHDRAWAL_REASON_REQUIRED', 'Withdrawal requires a reason.');
  return source;
}

function rejectSensitiveDocument({ filename = '', documentType = '', confidentiality = '', text = '' }) {
  const namedSensitive = /(?:member|membership|pastoral|prayer|disciplin|credential|secret|token|profile|admin(?:istrator)?[-_ ]?(?:mail|correspondence))/i;
  if (namedSensitive.test(filename) || namedSensitive.test(documentType) ||
      /^(?:MEMBERSHIP_REGISTER|PERSONAL_PROFILE|PASTORAL_RECORD|PRAYER_REQUEST|DISCIPLINARY_RECORD|CREDENTIAL|ADMIN_CORRESPONDENCE|RESTRICTED_CHURCH_RECORD)$/i.test(confidentiality)) {
    throw new KnowledgeError('SENSITIVE_SOURCE_EXCLUDED', 'This source type is excluded from the theological knowledge system.');
  }
  if (typeof text !== 'string' || !text.trim() || text.length > MAX_SOURCE_TEXT) {
    throw new KnowledgeError('INVALID_DOCUMENT_TEXT', 'Document text is empty or exceeds the ingestion limit.');
  }
  // Conservative identifiers screen; uncertain material remains un-ingested.
  if (/(?:\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b|\+?\d[\d\s().-]{7,}\d)/i.test(text)) {
    throw new KnowledgeError('POSSIBLE_PERSONAL_DATA', 'Possible personal identifiers require classification before ingestion.');
  }
}

function splitSections(text) {
  const lines = text.replace(/\r\n?/g, '\n').split('\n');
  const sections = [];
  let heading = 'Document';
  let body = [];
  const flush = () => {
    const content = body.join('\n').trim();
    if (content) sections.push({ heading, content });
    body = [];
  };
  for (const line of lines) {
    const m = line.match(/^\s{0,3}(#{1,6})\s+(.+?)\s*#*\s*$/);
    if (m) { flush(); heading = m[2].trim(); }
    else body.push(line);
  }
  flush();
  return sections;
}

function chunkDocument(text, source) {
  validateSource(source);
  rejectSensitiveDocument({ filename: source.sourceLocation, documentType: source.documentType, confidentiality: source.confidentiality, text });
  if (sha256(text) !== source.contentHash) throw new KnowledgeError('SOURCE_HASH_MISMATCH', 'Extracted text does not match the registered source hash.');
  const chunks = [];
  for (const section of splitSections(text)) {
    const paragraphs = section.content.split(/\n\s*\n/).map(x => x.trim()).filter(Boolean);
    let batch = [];
    let length = 0;
    const emit = () => {
      if (!batch.length) return;
      const content = batch.join('\n\n');
      const location = `${section.heading}${source.sectionLocation ? ` · ${source.sectionLocation}` : ''}`;
      const stable = `${source.sourceId}\0${source.version}\0${location}\0${content}`;
      chunks.push({
        chunkId: sha256(stable), sourceId: source.sourceId, sourceVersion: source.version,
        section: section.heading, location, content, contentHash: sha256(content),
        approvalStatus: source.status, authorityClass: source.authorityClass
      });
    };
    for (const original of paragraphs) {
      let parts = original.match(/.{1,1100}(?:\s|$)/g) || [original];
      parts = parts.map(x => x.trim()).filter(Boolean);
      for (const part of parts) {
        if (length + part.length + 2 > MAX_CHUNK_CHARS && batch.length) {
          emit();
          const overlap = batch.slice(-CHUNK_OVERLAP_PARAGRAPHS);
          batch = overlap;
          length = overlap.reduce((n, x) => n + x.length + 2, 0);
        }
        batch.push(part);
        length += part.length + 2;
      }
    }
    emit();
  }
  return chunks;
}

function isEligible(source, { purpose = INSTITUTIONAL_PURPOSE, audience = 'PUBLIC', permissions = [] } = {}) {
  if (!source || source.status !== 'APPROVED' || source.publicationStatus !== 'PUBLISHED' || SENSITIVE_CLASSIFICATIONS.has(String(source.confidentiality || '').toUpperCase())) return false;
  if (!Array.isArray(source.permittedUses) || !source.permittedUses.includes(purpose)) return false;
  if (!Array.isArray(source.permittedAudiences) || !source.permittedAudiences.includes(audience)) return false;
  if (String(source.authorityClass || '').toUpperCase() === 'PERSONAL_THEOLOGY' && purpose === INSTITUTIONAL_PURPOSE) return false;
  if (source.confidentiality === 'RESTRICTED' && !permissions.includes(`knowledge.source.${source.sourceId}.read`)) return false;
  return true;
}

function tokenize(text) {
  return (String(text || '').toLowerCase().match(/[\p{L}\p{N}]{3,}/gu) || []);
}

function rankChunks(query, chunks, sources, options = {}) {
  const terms = [...new Set(tokenize(query))];
  if (!terms.length) return [];
  const sourceMap = sources instanceof Map ? sources : new Map((sources || []).map(s => [`${s.sourceId}\0${s.version}`, s]));
  return (chunks || []).filter(chunk => {
    const source = sourceMap.get(`${chunk.sourceId}\0${chunk.sourceVersion}`);
    return source && isEligible(source, options) && sha256(chunk.content) === chunk.contentHash;
  }).map(chunk => {
    const haystack = `${chunk.section || ''} ${chunk.content}`.toLowerCase();
    const score = terms.reduce((sum, term) => sum + (haystack.includes(term) ? 1 : 0), 0);
    return { ...chunk, score, source: sourceMap.get(`${chunk.sourceId}\0${chunk.sourceVersion}`) };
  }).filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score || a.source.sourceId.localeCompare(b.source.sourceId) || a.location.localeCompare(b.location) || a.chunkId.localeCompare(b.chunkId))
    .slice(0, Number.isSafeInteger(options.maxResults) && options.maxResults > 0 ? options.maxResults : MAX_CONTEXT_PASSAGES);
}

function buildContextPackage(query, ranked, unresolvedConflicts = []) {
  let total = 0;
  const passages = [];
  const groupSources = new Map();
  for (const item of ranked) {
    const group = item.source.conflictGroup;
    if (!group) continue;
    if (!groupSources.has(group)) groupSources.set(group, new Set());
    groupSources.get(group).add(item.source.sourceId);
  }
  const resolvedConflictMap = new Map([...groupSources.entries()].filter(([, ids]) => ids.size > 1));
  const matchedGroups = new Set(ranked.map(item => item.source.conflictGroup).filter(Boolean));
  for (const conflict of unresolvedConflicts) {
    if (!matchedGroups.has(conflict.conflictGroup) || !Array.isArray(conflict.sourceIds) || conflict.sourceIds.length < 2) continue;
    resolvedConflictMap.set(conflict.conflictGroup, new Set(conflict.sourceIds));
  }
  const conflictGroups = [...resolvedConflictMap.entries()];
  const contestedIds = new Set(conflictGroups.flatMap(([, ids]) => [...ids]));
  const prioritized = [];
  const seenContested = new Set();
  for (const item of ranked) {
    if (contestedIds.has(item.source.sourceId) && !seenContested.has(item.source.sourceId)) {
      prioritized.push(item);
      seenContested.add(item.source.sourceId);
    }
  }
  const ordered = [...prioritized, ...ranked.filter(item => !seenContested.has(item.source.sourceId))];
  for (const item of ordered) {
    if (passages.length >= MAX_CONTEXT_PASSAGES) break;
    const source = item.source;
    const passage = {
      sourceId: source.sourceId, title: source.title, version: source.version,
      authorityClass: source.authorityClass, approvalStatus: source.status, publicationStatus: source.publicationStatus,
      approvalReference: source.approvalReference, conflictGroup: source.conflictGroup || null, location: item.location,
      contentHash: item.contentHash, text: item.content
    };
    const size = JSON.stringify(passage).length;
    if (total + size > MAX_CONTEXT_CHARS) continue;
    total += size;
    passages.push(passage);
  }
  const conflicts = conflictGroups.map(([conflictGroup, ids]) => ({ conflictGroup, sourceIds: [...ids].sort() }));
  return Object.freeze({ purpose: INSTITUTIONAL_PURPOSE, queryHash: sha256(query), passages: Object.freeze(passages), scriptures: Object.freeze([]), conflicts: Object.freeze(conflicts), sufficient: passages.length > 0 });
}

function classifyResponseCategory(category) {
  return ['PASSAGE_INTERPRETATION', 'THEOLOGICAL_DEFINITION', 'BIBLICAL_LIFE_EXAMPLE'].includes(category)
    ? category : 'OUT_OF_SCOPE';
}

function createResponseContract({ category, sourceReferences = [], scriptureReferences = [], confidence = 'INSUFFICIENT', escalationRequired = false, limitationReason = null, answer = '' }) {
  const normalizedCategory = classifyResponseCategory(category);
  const eligibleRefs = sourceReferences.filter(ref => ref && ref.approvalStatus === 'APPROVED' && ref.sourceId && ref.version && ref.location);
  const allowed = normalizedCategory !== 'OUT_OF_SCOPE' && (normalizedCategory === 'BIBLICAL_LIFE_EXAMPLE' || eligibleRefs.length > 0 || scriptureReferences.length > 0);
  return Object.freeze({
    category: normalizedCategory, sourceReferences: eligibleRefs,
    scriptureReferences: scriptureReferences.filter(Boolean), confidence: ['HIGH', 'MEDIUM', 'LOW', 'INSUFFICIENT'].includes(confidence) ? confidence : 'INSUFFICIENT',
    escalationRequired: Boolean(escalationRequired || !allowed), limitationReason: allowed ? limitationReason : (limitationReason || 'NO_ELIGIBLE_SOURCE'),
    answer: allowed ? String(answer || '') : '', audit: Object.freeze({ sourceCount: eligibleRefs.length, scriptureCount: scriptureReferences.length })
  });
}

function parseScriptureReference(value) {
  if (typeof value !== 'string') return null;
  const match = value.trim().match(/^(.+?)\s+(\d{1,3}):(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?$/);
  if (!match) return null;
  const start = Number(match[3]);
  const end = match[4] ? Number(match[4]) : start;
  const chapter = Number(match[2]);
  if (!Number.isInteger(start) || !Number.isInteger(end) || chapter < 1 || start < 1 || end < start || end - start > 49) return null;
  return { book: match[1].trim(), chapter, startVerse: start, endVerse: end };
}

function lookupScripture(reference, translationId, corpus) {
  const parsed = parseScriptureReference(reference);
  if (!parsed) return { ok: false, code: 'INVALID_REFERENCE' };
  if (!corpus || !Array.isArray(corpus.verses) || !Array.isArray(corpus.books) ||
      !corpus.integrityVerified || !corpus.corpusId || !corpus.version) return { ok: false, code: 'CORPUS_UNAVAILABLE' };
  const translation = String(translationId || '').toUpperCase();
  if (!corpus.translations || !corpus.translations.includes(translation) ||
      !corpus.translationLicenses || corpus.translationLicenses[translation] !== 'AUTHORIZED') return { ok: false, code: 'TRANSLATION_UNAVAILABLE' };
  const book = corpus.books.find(item => item.name.toLowerCase() === parsed.book.toLowerCase() || (item.aliases || []).some(alias => alias.toLowerCase() === parsed.book.toLowerCase()));
  if (!book || parsed.chapter > book.chapterCount) return { ok: false, code: 'INVALID_REFERENCE' };
  const verses = corpus.verses.filter(v => v.translationId === translation && v.bookNumber === book.bookNumber && v.chapterNumber === parsed.chapter && v.verseNumber >= parsed.startVerse && v.verseNumber <= parsed.endVerse).sort((a, b) => a.verseNumber - b.verseNumber);
  if (verses.length !== parsed.endVerse - parsed.startVerse + 1) return { ok: false, code: 'INVALID_REFERENCE' };
  if (verses.some(v => !v.contentHash || sha256(v.text) !== v.contentHash)) return { ok: false, code: 'CORPUS_INTEGRITY_FAILURE' };
  return { ok: true, book: book.name, chapter: parsed.chapter, startVerse: parsed.startVerse, endVerse: parsed.endVerse, translationId: translation, corpusId: corpus.corpusId, corpusVersion: corpus.version, text: verses.map(v => v.text).join(' '), verses: verses.map(v => ({ verse: v.verseNumber, text: v.text })) };
}

async function ingestSource(client, metadata, extractedText, { actorId = null, requestId = null, now = () => Date.now() } = {}) {
  if (!client || typeof client.query !== 'function') throw new KnowledgeError('INGESTION_STORE_REQUIRED', 'An isolated database transaction is required.');
  const input = metadata || {};
  const source = {
    sourceId: input.sourceId, title: input.title, documentType: input.documentType,
    issuingAuthority: input.issuingAuthority || null, authorityClass: input.authorityClass,
    scopeOfAuthority: input.scopeOfAuthority || null, version: input.version,
    status: 'UNVERIFIED', confidentiality: input.confidentiality,
    confidentialityReviewer: input.confidentialityReviewer,
    confidentialityReviewedAt: input.confidentialityReviewedAt,
    permittedAudiences: input.permittedAudiences, permittedUses: input.permittedUses,
    sourceLocation: input.sourceLocation, contentHash: sha256(extractedText),
    language: input.language, translationId: input.translationId || null,
    ingestedAt: now(), publicationDate: input.publicationDate || null,
    supersedesSourceId: input.supersedesSourceId || null,
    supersedesVersion: input.supersedesVersion || null
  };
  validateSource(source);
  rejectSensitiveDocument({ filename: source.sourceLocation, documentType: source.documentType, confidentiality: source.confidentiality, text: extractedText });
  const chunks = chunkDocument(extractedText, source);
  const result = await client.query(
    `INSERT INTO ai_knowledge_sources
      (source_id, version, title, document_type, issuing_authority, authority_class, scope_of_authority, conflict_group,
       publication_date, approval_status, confidentiality, permitted_audiences, permitted_uses,
       confidentiality_reviewed_by, confidentiality_reviewed_at, source_location, content_hash, language, translation_id, ingested_at, created_by,
       supersedes_source_id, supersedes_version, updated_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,'UNVERIFIED',$10,$11::jsonb,$12::jsonb,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23)
     RETURNING source_id, version, approval_status, content_hash`,
    [source.sourceId, source.version, source.title, source.documentType, source.issuingAuthority,
      source.authorityClass, source.scopeOfAuthority, null, source.publicationDate, source.confidentiality,
      JSON.stringify(source.permittedAudiences), JSON.stringify(source.permittedUses),
      source.confidentialityReviewer, source.confidentialityReviewedAt, source.sourceLocation,
      source.contentHash, source.language, source.translationId, source.ingestedAt, actorId,
      source.supersedesSourceId, source.supersedesVersion, source.ingestedAt]
  );
  for (const chunk of chunks) {
    await client.query(
      `INSERT INTO ai_knowledge_chunks
        (chunk_id, source_id, source_version, section, location, content, content_hash, created_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      [chunk.chunkId, chunk.sourceId, chunk.sourceVersion, chunk.section, chunk.location,
        chunk.content, chunk.contentHash, source.ingestedAt]
    );
  }
  await client.query(
    `INSERT INTO ai_knowledge_events (event_id, source_id, source_version, event_type, actor_id, actor_role, reason, created_at, request_id)
     VALUES ($1,$2,$3,'INGESTED',$4,'SOURCE_ADMIN','SOURCE_REGISTERED_UNVERIFIED',$5,$6)`,
    [crypto.randomUUID(), source.sourceId, source.version, actorId, source.ingestedAt, requestId]
  );
  return { source: result.rows[0], chunkCount: chunks.length };
}

async function retrieveInstitutionalContext(queryFn, question) {
  if (typeof queryFn !== 'function' || typeof question !== 'string' || !question.trim()) {
    return buildContextPackage(String(question || ''), []);
  }
  const { rows: sources } = await queryFn(
    `SELECT source_id, version, title, authority_class, approval_status, publication_status, approval_reference, conflict_group,
            confidentiality, permitted_audiences, permitted_uses, effective_at, content_hash
       FROM ai_knowledge_sources
      WHERE approval_status = 'APPROVED'
        AND publication_status = 'PUBLISHED'
        AND confidentiality = 'PUBLIC'
        AND permitted_audiences @> '["PUBLIC"]'::jsonb
        AND permitted_uses @> '["INSTITUTIONAL"]'::jsonb
        AND (effective_at IS NULL OR effective_at <= $1)
        AND authority_class <> 'PERSONAL_THEOLOGY'
      ORDER BY authority_class, source_id, version`, [Date.now()]
  );
  if (!sources.length) return buildContextPackage(question, []);
  const byIdVersion = new Map(sources.map(s => [`${s.source_id}\0${s.version}`, {
    sourceId: s.source_id, version: s.version, title: s.title,
    authorityClass: s.authority_class, status: s.approval_status, publicationStatus: s.publication_status,
    approvalReference: s.approval_reference, conflictGroup: s.conflict_group, confidentiality: s.confidentiality,
    permittedAudiences: s.permitted_audiences, permittedUses: s.permitted_uses,
    contentHash: s.content_hash
  }]));
  const { rows: storedChunks } = await queryFn(
    `SELECT c.chunk_id, c.source_id, c.source_version, c.section, c.location,
            c.content, c.content_hash
       FROM ai_knowledge_chunks c
       JOIN ai_knowledge_sources s ON s.source_id = c.source_id AND s.version = c.source_version
      WHERE s.approval_status = 'APPROVED'
        AND s.publication_status = 'PUBLISHED'
        AND s.confidentiality = 'PUBLIC'
        AND s.permitted_audiences @> '["PUBLIC"]'::jsonb
        AND s.permitted_uses @> '["INSTITUTIONAL"]'::jsonb
        AND (s.effective_at IS NULL OR s.effective_at <= $1)
        AND s.authority_class <> 'PERSONAL_THEOLOGY'
      ORDER BY c.source_id, c.source_version, c.section, c.chunk_id`, [Date.now()]
  );
  const chunks = storedChunks.map(c => ({
    chunkId: c.chunk_id, sourceId: c.source_id, sourceVersion: c.source_version,
    section: c.section, location: c.location, content: c.content,
    contentHash: c.content_hash
  }));
  const sourceRows = sources.map(s => byIdVersion.get(`${s.source_id}\0${s.version}`));
  const options = { purpose: INSTITUTIONAL_PURPOSE, audience: 'PUBLIC', maxResults: MAX_RETRIEVAL_CANDIDATES };
  const ranked = rankChunks(question, chunks, sourceRows, options);
  let unresolvedConflicts = [];
  const matchedConflictGroups = [...new Set(ranked.map(item => item.source.conflictGroup).filter(Boolean))];
  if (matchedConflictGroups.length) {
    const { rows } = await queryFn(
      `SELECT g.conflict_group, cs.source_id
         FROM ai_knowledge_conflict_groups g
         JOIN ai_knowledge_conflict_sources cs ON cs.conflict_group=g.conflict_group
        WHERE g.status='UNRESOLVED' AND g.conflict_group = ANY($1::text[])
        ORDER BY g.conflict_group, cs.source_id`, [matchedConflictGroups]
    );
    const members = new Map();
    for (const row of rows) {
      if (!members.has(row.conflict_group)) members.set(row.conflict_group, new Set());
      members.get(row.conflict_group).add(row.source_id);
    }
    unresolvedConflicts = [...members.entries()].map(([conflictGroup, ids]) => ({ conflictGroup, sourceIds: [...ids] }));
  }
  const activeConflictGroups = new Set(ranked.map(item => item.source.conflictGroup).filter(Boolean));
  const included = new Set(ranked.map(item => `${item.sourceId}\0${item.sourceVersion}\0${item.chunkId}`));
  for (const chunk of chunks) {
    const source = byIdVersion.get(`${chunk.sourceId}\0${chunk.sourceVersion}`);
    const key = `${chunk.sourceId}\0${chunk.sourceVersion}\0${chunk.chunkId}`;
    if (source && activeConflictGroups.has(source.conflictGroup) && !included.has(key) && isEligible(source, options) && sha256(chunk.content) === chunk.contentHash) {
      ranked.push({ ...chunk, score: 0, source });
      included.add(key);
    }
  }
  return buildContextPackage(question, ranked, unresolvedConflicts);
}

module.exports = {
  SOURCE_STATUSES, SENSITIVE_CLASSIFICATIONS, CONFIDENTIALITY_LEVELS, INSTITUTIONAL_PURPOSE,
  MAX_CHUNK_CHARS, MAX_CONTEXT_PASSAGES, MAX_CONTEXT_CHARS, MAX_RETRIEVAL_CANDIDATES,
  KnowledgeError, sha256, validateSource, rejectSensitiveDocument,
  chunkDocument, isEligible, rankChunks, buildContextPackage,
  classifyResponseCategory, createResponseContract,
  parseScriptureReference, lookupScripture, ingestSource, retrieveInstitutionalContext
};
