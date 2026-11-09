'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { isPostgresActive, memoryStore, query } = require('../db');

const NOTICE_PATH = path.join(__dirname, '../../privacy-notice-v0.1.md');
const NOTICE_VERSION = '0.1';
const ACKNOWLEDGEMENT_TEXT = 'I acknowledge that PMCI has provided me with the Perazim App Privacy Notice and that I can read the full PMCI Data Protection and Privacy Policy here. I understand how to contact PMCI about my personal information.';

function readNoticeText() {
  return fs.readFileSync(NOTICE_PATH, 'utf8');
}

function hashContent(content) {
  return crypto.createHash('sha256').update(content, 'utf8').digest('hex');
}

async function getCurrentNotice() {
  if (!isPostgresActive()) {
    const testNotice = process.env.NODE_ENV === 'test' ? memoryStore.data01TestPrivacyNotice : null;
    if (testNotice) {
      if (hashContent(testNotice.content) !== testNotice.content_hash) {
        throw new Error('Synthetic test notice content does not match its recorded hash');
      }
      return { ...testNotice, presented_at: Date.now() };
    }
    const content = readNoticeText();
    return {
      version: NOTICE_VERSION,
      content_hash: hashContent(content),
      publication_status: 'DRAFT',
      effective_at: null,
      presented_at: Date.now(),
      content
    };
  }

  const result = await query(
    `SELECT version,content_hash,document_path,publication_status,effective_at,withdrawn_at
       FROM privacy_notice_versions WHERE is_active=TRUE LIMIT 1`
  );
  const row = result.rows[0];
  if (!row) throw new Error('No active privacy notice is configured');
  const documentPath = path.resolve(__dirname, '../..', row.document_path);
  if (path.dirname(documentPath) !== path.resolve(__dirname, '../..')) {
    throw new Error('Privacy notice path is outside the approved document directory');
  }
  const content = fs.readFileSync(documentPath, 'utf8');
  if (hashContent(content) !== row.content_hash.trim()) {
    throw new Error('Active privacy notice content does not match its recorded hash');
  }
  return { ...row, presented_at: Date.now(), content };
}

async function recordAcknowledgement(client, userId, notice, acknowledgedAt) {
  // The server stamps the notice when it validates registration. Never accept
  // a presentation timestamp from the client as acknowledgement evidence.
  const serverPresentedAt = Number(notice.presented_at);
  if (!Number.isSafeInteger(serverPresentedAt) || serverPresentedAt <= 0) {
    throw new Error('The server could not establish a privacy notice presentation time');
  }
  await client.query(
    `INSERT INTO privacy_notice_acknowledgements
      (user_id,notice_version,content_hash,presented_at,acknowledged_at,acknowledgement_text,client_platform)
     VALUES ($1,$2,$3,$4,$5,$6,'android')`,
    [userId, notice.version, notice.content_hash, serverPresentedAt, acknowledgedAt, ACKNOWLEDGEMENT_TEXT]
  );
}

module.exports = {
  ACKNOWLEDGEMENT_TEXT,
  getCurrentNotice,
  hashContent,
  recordAcknowledgement
};
