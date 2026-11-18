'use strict';

const { extractScriptureReferences } = require('./scriptureCorpus');
const { sha256 } = require('./governedKnowledge');

const CATEGORIES = new Set(['PASSAGE_INTERPRETATION', 'THEOLOGICAL_DEFINITION', 'BIBLICAL_LIFE_EXAMPLE']);
const OUT_OF_SCOPE = /\b(?:diagnos(?:e|is|ed|ing)|prescrib(?:e|ing)|treatment plan|medical advice|legal advice|financial advice|invest(?:ment|ing) advice|you have (?:depression|bipolar|ptsd|anxiety disorder)|stop taking (?:your )?medication|sue them|declare bankruptcy)\b/i;
const PERSONAL_TESTIMONY = /\b(?:in my (?:pastoral|clinical) experience|I once (?:counseled|counselled|treated|helped)|I (?:have|had) (?:counselled|counseled|treated|helped|seen|met|experienced)|my personal testimony|a member of our church|someone at our church told me|I know someone who)\b/i;
const INSTITUTIONAL_CLAIM = /\b(?:Perazim(?: Mission Church)?|our church|the church|church leaders?|church leadership|church constitution|church policy)\b.{0,100}\b(?:official(?:ly)?|adopted|approved|teaches|believes|says|declares|requires|prohibits|forbids|permits|allows|mandates|recognizes|recognises|holds|affirms|states|position|teaching|doctrine|view|rule|policy)\b|\b(?:official|adopted|approved)\b.{0,60}\b(?:Perazim|church)\b|\baccording to (?:Perazim|our church|the church|church policy|the church constitution)\b/i;
const QUOTES = /[“"]([^”"]{1,600})[”"]/g;

function sourceKey(ref) { return `${ref.sourceId}\0${ref.version}\0${ref.location}`; }
function scriptureKey(ref) { return `${ref.reference}\0${ref.translationId}\0${ref.corpusId}\0${ref.corpusVersion}\0${ref.corpusHash}`; }
function fail(code) {
  return { valid: false, code, escalationRequired: true,
    answer: 'I cannot verify a safe, properly sourced answer to this question from the available approved material. Please ask an appropriate church authority for guidance.' };
}

function validateModelResponse(candidate, context) {
  if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) return fail('INVALID_RESPONSE');
  const { category, answer, sourceReferences, scriptureReferences } = candidate;
  if (candidate.escalationRequired === true) return fail('MODEL_REQUESTED_ESCALATION');
  if (!CATEGORIES.has(category)) return fail('OUT_OF_SCOPE');
  if (typeof answer !== 'string' || !answer.trim() || answer.length > 2400 ||
      !Array.isArray(sourceReferences) || sourceReferences.length > 6 ||
      !Array.isArray(scriptureReferences) || scriptureReferences.length < 1 || scriptureReferences.length > 3) return fail('INVALID_RESPONSE');
  if (OUT_OF_SCOPE.test(answer)) return fail('PROHIBITED_ADVICE');
  if (PERSONAL_TESTIMONY.test(answer)) return fail('FABRICATED_TESTIMONY');

  const sources = new Map((context?.passages || []).map(item => [sourceKey(item), item]));
  const scriptures = new Map((context?.scriptures || []).map(item => [scriptureKey(item), item]));
  const validatedSources = [];
  for (const ref of sourceReferences) {
    if (!ref || typeof ref.sourceId !== 'string' || typeof ref.version !== 'string' || typeof ref.location !== 'string') return fail('UNVERIFIED_SOURCE_REFERENCE');
    const exact = sources.get(sourceKey(ref));
    if (!exact || exact.approvalStatus !== 'APPROVED' || exact.publicationStatus !== 'PUBLISHED' ||
        typeof exact.text !== 'string' || typeof exact.contentHash !== 'string' || sha256(exact.text) !== exact.contentHash) {
      return fail('UNVERIFIED_SOURCE_REFERENCE');
    }
    validatedSources.push({ sourceId: exact.sourceId, title: exact.title, version: exact.version,
      authorityClass: exact.authorityClass, approvalStatus: exact.approvalStatus,
      approvalReference: exact.approvalReference, publicationStatus: exact.publicationStatus,
      location: exact.location, contentHash: exact.contentHash });
  }
  if (!validatedSources.length) return fail('SOURCE_PROVENANCE_REQUIRED');
  const validatedScriptures = [];
  for (const ref of scriptureReferences) {
    if (!ref || typeof ref.reference !== 'string' || typeof ref.translationId !== 'string' ||
        typeof ref.corpusId !== 'string' || typeof ref.corpusVersion !== 'string' || typeof ref.corpusHash !== 'string' || typeof ref.quote !== 'string') return fail('UNVERIFIED_SCRIPTURE_REFERENCE');
    const exact = scriptures.get(scriptureKey(ref));
    if (!exact || typeof exact.text !== 'string' || !Array.isArray(exact.verses) ||
        exact.verses.length !== exact.endVerse - exact.startVerse + 1 ||
        exact.verses.some((verse, index) => verse.verse !== exact.startVerse + index ||
          typeof verse.text !== 'string' || sha256(verse.text) !== verse.contentHash) ||
        exact.text !== exact.verses.map(verse => verse.text).join(' ') ||
        !exact.text.includes(ref.quote) || ref.quote.length < 4 || ref.quote.length > 1200) return fail('UNVERIFIED_SCRIPTURE_REFERENCE');
    validatedScriptures.push({ reference: exact.reference, book: exact.book, chapter: exact.chapter,
      startVerse: exact.startVerse, endVerse: exact.endVerse, translationId: exact.translationId,
      corpusId: exact.corpusId, corpusVersion: exact.corpusVersion, corpusHash: exact.corpusHash,
      text: exact.text, verses: exact.verses, quote: ref.quote });
  }

  const institutionalClaim = INSTITUTIONAL_CLAIM.test(answer);
  if (institutionalClaim) {
    // Lexical overlap cannot prove that a source entails an institutional claim.
    // Until a reviewed claim-to-source rule exists, fail closed even when a
    // published official source is present in the retrieval context.
    return fail('UNSUPPORTED_INSTITUTIONAL_CLAIM');
  }
  const allowedEvidence = [
    ...validatedScriptures.map(ref => scriptures.get(scriptureKey(ref))),
    ...validatedSources.map(ref => sources.get(sourceKey(ref)))
  ];
  for (const match of answer.matchAll(QUOTES)) {
    if (!allowedEvidence.some(evidence => evidence.text.includes(match[1]))) return fail('UNSUPPORTED_QUOTATION');
  }
  for (const ref of extractScriptureReferences(answer)) {
    if (!validatedScriptures.some(allowed => allowed.reference.toLowerCase() === ref.reference.toLowerCase())) return fail('UNVERIFIED_SCRIPTURE_REFERENCE');
  }
  return {
    valid: true, code: 'VALIDATED', escalationRequired: false,
    response: { category, answer: answer.trim(), sourceReferences: validatedSources,
      scriptureReferences: validatedScriptures, confidence: 'LOW',
      escalationRequired: false }
  };
}

module.exports = { CATEGORIES, OUT_OF_SCOPE, INSTITUTIONAL_CLAIM, validateModelResponse };
