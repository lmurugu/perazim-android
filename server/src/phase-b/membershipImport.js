'use strict';

const crypto = require('crypto');
const { validateAndNormalizePhone } = require('../routes/auth');

function normalizeName(value) {
  return typeof value === 'string' ? value.trim().replace(/\s+/g, ' ').toLowerCase() : '';
}

function fingerprint(value) {
  const key = process.env.DATA01_IMPORT_FINGERPRINT_KEY ||
    (process.env.NODE_ENV === 'test' ? 'perazim-synthetic-import-fingerprint-key-v1' : null);
  if (!key) throw new Error('Membership import fingerprint key is not configured');
  return crypto.createHmac('sha256', key).update(JSON.stringify(value)).digest('hex');
}

function prepareImportRows(records) {
  return records.map((record, index) => {
    const row = {
      rowNumber: index + 1,
      sourceReference: null,
      fullName: null,
      phoneValue: null,
      normalizedPhone: null,
      issues: [],
      sourceShapeValid: !!record && typeof record === 'object' && !Array.isArray(record)
    };
    if (!row.sourceShapeValid) {
      row.issues.push('INVALID_ROW');
      row.rowStatus = 'REJECTED';
      row.fingerprint = fingerprint({ invalid_row: index + 1, value: record });
      return row;
    }

    const rawReference = record.reference;
    const rawName = record.full_name;
    const rawPhone = record.phone;
    if (typeof rawReference === 'string') {
      const reference = rawReference.trim();
      if (reference.length <= 100) row.sourceReference = reference || null;
      else row.issues.push('REFERENCE_TOO_LONG');
    } else if (rawReference != null) row.issues.push('MALFORMED_REFERENCE');

    if (typeof rawName === 'string') {
      const name = rawName.trim().replace(/\s+/g, ' ');
      if (name.length <= 128) row.fullName = name || null;
      else row.issues.push('NAME_TOO_LONG');
    } else if (rawName != null) row.issues.push('MALFORMED_NAME');

    if (typeof rawPhone === 'string') {
      const phone = rawPhone.trim();
      if (phone.length <= 64) {
        row.phoneValue = phone || null;
        row.normalizedPhone = phone ? validateAndNormalizePhone(phone) : null;
        if (phone && !row.normalizedPhone) row.issues.push('MALFORMED_PHONE');
      } else row.issues.push('PHONE_TOO_LONG');
    } else if (rawPhone != null) row.issues.push('MALFORMED_PHONE');

    if (!row.sourceReference) row.issues.push('MISSING_REFERENCE');
    if (!row.fullName) row.issues.push('MISSING_NAME');
    if (!row.phoneValue) row.issues.push('MISSING_PHONE');

    const rejected = row.issues.some(issue => issue.endsWith('_TOO_LONG') || issue.startsWith('MALFORMED_'));
    row.rowStatus = rejected ? 'REJECTED' : row.issues.length ? 'INCOMPLETE' : 'ACCEPTED';
    row.fingerprint = fingerprint({
      source_reference: row.sourceReference,
      full_name: row.fullName,
      phone_value: row.phoneValue,
      row_number: row.rowNumber,
      issues: row.issues
    });
    return row;
  });
}

function classifyImportRows(rows, { existingReferences = new Set(), existingPhones = new Set(), existingIdentities = new Set(), existingReferenceIdentities = new Map(), changedBatchReplay = false } = {}) {
  const refs = new Map();
  const referenceIdentities = new Map();
  const phones = new Map();
  const identities = new Map();
  for (const row of rows) {
    if (row.sourceReference) {
      const key = row.sourceReference.toLowerCase();
      refs.set(key, (refs.get(key) || 0) + 1);
      if (!referenceIdentities.has(key)) referenceIdentities.set(key, new Set());
      referenceIdentities.get(key).add(`${normalizeName(row.fullName)}\u0000${row.normalizedPhone || ''}`);
    }
    if (row.normalizedPhone) phones.set(row.normalizedPhone, (phones.get(row.normalizedPhone) || 0) + 1);
    const identity = row.fullName && row.normalizedPhone ? `${normalizeName(row.fullName)}\u0000${row.normalizedPhone}` : null;
    if (identity) identities.set(identity, (identities.get(identity) || 0) + 1);
  }

  for (const row of rows) {
    if (row.rowStatus === 'REJECTED') continue;
    if (changedBatchReplay) row.issues.push('CHANGED_BATCH_REPLAY');
    if (row.sourceReference) {
      const referenceKey = row.sourceReference.toLowerCase();
      if ((refs.get(referenceKey) || 0) > 1 || existingReferences.has(referenceKey)) row.issues.push('DUPLICATE_REFERENCE');
      const rowIdentity = `${normalizeName(row.fullName)}\u0000${row.normalizedPhone || ''}`;
      const batchReferenceIdentities = referenceIdentities.get(referenceKey) || new Set();
      const storedReferenceIdentities = existingReferenceIdentities.get(referenceKey) || new Set();
      if ([...batchReferenceIdentities, ...storedReferenceIdentities].some(identity => identity !== rowIdentity)) {
        row.issues.push('IDENTITY_CONFLICT');
      }
    }
    if (row.normalizedPhone && ((phones.get(row.normalizedPhone) || 0) > 1 || existingPhones.has(row.normalizedPhone))) {
      row.issues.push('SHARED_PHONE');
    }
    const identity = row.fullName && row.normalizedPhone ? `${normalizeName(row.fullName)}\u0000${row.normalizedPhone}` : null;
    if (identity && ((identities.get(identity) || 0) > 1 || existingIdentities.has(identity))) row.issues.push('POSSIBLE_DUPLICATE_PERSON');

    row.rowStatus = changedBatchReplay || row.issues.some(issue => ['DUPLICATE_REFERENCE', 'SHARED_PHONE', 'POSSIBLE_DUPLICATE_PERSON', 'IDENTITY_CONFLICT'].includes(issue))
      ? 'REVIEW'
      : row.issues.length ? 'INCOMPLETE' : 'ACCEPTED';
  }

  const counts = {
    accepted: 0, incomplete: 0, duplicate_reference: 0, shared_phone: 0,
    possible_duplicate: 0, identity_conflict: 0, changed_batch_replay: 0, rejected: 0
  };
  for (const row of rows) {
    if (row.rowStatus === 'ACCEPTED') counts.accepted++;
    else if (row.rowStatus === 'INCOMPLETE') counts.incomplete++;
    else if (row.rowStatus === 'REJECTED') counts.rejected++;
    if (row.issues.includes('DUPLICATE_REFERENCE')) counts.duplicate_reference++;
    if (row.issues.includes('SHARED_PHONE')) counts.shared_phone++;
    if (row.issues.includes('POSSIBLE_DUPLICATE_PERSON')) counts.possible_duplicate++;
    if (row.issues.includes('IDENTITY_CONFLICT')) counts.identity_conflict++;
    if (row.issues.includes('CHANGED_BATCH_REPLAY')) counts.changed_batch_replay++;
  }
  return { rows, counts };
}

function batchFingerprint(source, rows) {
  return fingerprint({ source: String(source).trim(), rows: rows.map(row => ({
    source_reference: row.sourceReference, full_name: row.fullName, phone_value: row.phoneValue,
    normalized_phone: row.normalizedPhone, issues: row.issues, row_status: row.rowStatus,
    row_fingerprint: row.fingerprint
  })) });
}

module.exports = { batchFingerprint, classifyImportRows, normalizeName, prepareImportRows };
