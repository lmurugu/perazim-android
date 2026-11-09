'use strict';

const { extractScriptureReferences } = require('./scriptureCorpus');

const CATEGORIES = new Set(['PASSAGE_INTERPRETATION', 'THEOLOGICAL_DEFINITION', 'BIBLICAL_LIFE_EXAMPLE']);
const OUT_OF_SCOPE = /\b(?:diagnos(?:e|is|ed|ing)|prescrib(?:e|ing)|treatment plan|medical advice|legal advice|financial advice|invest(?:ment|ing) advice|you have (?:depression|bipolar|ptsd|anxiety disorder)|stop taking (?:your )?medication|sue them|declare bankruptcy)\b/i;
const PERSONAL_TESTIMONY = /\b(?:in my (?:pastoral|clinical) experience|I once (?:counseled|counselled|treated|helped)|I (?:have|had) (?:counselled|counseled|treated|helped|seen|met|experienced)|my personal testimony|a member of our church|someone at our church told me|I know someone who)\b/i;
const INSTITUTIONAL_CLAIM = /\b(?:Perazim(?: Mission Church)?(?:'s)?|our church|the church)(?:'s)?\s+(?:(?:official(?:ly)?|adopted|approved)\s+)?(?:teaches|believes|requires|prohibits|mandates|recognizes|recognises|holds|affirms|states|has adopted|has approved|position|teaching|doctrine|view|rule|beliefs?|policy)\b|\b(?:official (?:Perazim|church) position|church doctrine requires)\b/i;
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
    if (!exact || exact.approvalStatus !== 'APPROVED' || exact.publicationStatus !== 'PUBLISHED') return fail('UNVERIFIED_SOURCE_REFERENCE');
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
    if (!exact || !exact.text.includes(ref.quote) || ref.quote.length < 4 || ref.quote.length > 1200) return fail('UNVERIFIED_SCRIPTURE_REFERENCE');
    validatedScriptures.push({ reference: exact.reference, book: exact.book, chapter: exact.chapter,
      startVerse: exact.startVerse, endVerse: exact.endVerse, translationId: exact.translationId,
      corpusId: exact.corpusId, corpusVersion: exact.corpusVersion, corpusHash: exact.corpusHash,
      text: exact.text, verses: exact.verses, quote: ref.quote });
  }

  const institutionalClaim = INSTITUTIONAL_CLAIM.test(answer);
  if (institutionalClaim) {
    const officialRefs = validatedSources.filter(ref => ['OFFICIAL_CONSTITUTION', 'OFFICIAL_POLICY', 'OFFICIAL_RESOLUTION'].includes(ref.authorityClass));
    const claimSentence = answer.split(/[.!?\n]/).find(sentence => INSTITUTIONAL_CLAIM.test(sentence)) || answer;
    const claimTerms = new Set((claimSentence.toLowerCase().match(/[\p{L}]{4,}/gu) || []).filter(term => !['perazim','church','officially','teaches','believes','requires','prohibits','mandates','recognizes','recognises','holds','affirms','states','position','teaching','doctrine','view','rule'].includes(term)));
    const supported = officialRefs.some(ref => {
      const evidence = (sources.get(sourceKey(ref))?.text || '').toLowerCase();
      const overlap = [...claimTerms].filter(term => evidence.includes(term));
      return claimTerms.size === 0 || overlap.length >= Math.min(2, claimTerms.size);
    });
    if (!supported) return fail('UNSUPPORTED_INSTITUTIONAL_CLAIM');
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
