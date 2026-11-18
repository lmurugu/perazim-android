'use strict';

// Erasure of a member's personal data (plan C.2).
//
// Policy, applied in one database transaction:
//  - Personal content the member created is deleted: posts, comments, prayer
//    requests, amens, RSVPs, group memberships, connections, messages they sent or
//    received, notifications they received or triggered, challenge progress, uploaded media.
//  - The member's account row is anonymised, not deleted, so that accountability
//    records keep valid references. Login is disabled (is_active = false) and the PIN
//    hash is replaced with random data.
//  - Membership claims keep their decision and date for governance, but the name
//    and phone are replaced.
//  - Append-only accountability records (audit log, consent history, request
//    history, membership history, privacy acknowledgements) cannot be changed by
//    design. They are retained under the legal-obligation rule in the Privacy Policy.
//    They hold no free-text personal content.
//
// Returns the uploaded filenames so the caller can delete the files after commit.

const crypto = require('crypto');

// Every column that can point at a person, and what erasure does with it.
// The completeness test fails if a new person-linked column is added without an entry here.
const ERASURE_POLICY = {
  // Deleted: personal content and links owned by the member.
  'community_media.owner_id': 'delete',
  'community_posts.author_id': 'delete',
  'post_comments.author_id': 'delete',
  'post_amens.user_id': 'delete',
  'event_rsvps.user_id': 'delete',
  'post_reactions.user_id': 'delete',
  'comment_reactions.user_id': 'delete',
  'event_photos.uploader_id': 'delete',
  'event_photos.reviewed_by': 'keep: review record, reviewer link to the anonymised account',
  'group_memberships.user_id': 'delete',
  'in_app_notifications.user_id': 'delete',
  'in_app_notifications.actor_id': 'delete',
  'connections.requester_id': 'delete',
  'connections.recipient_id': 'delete',
  'messages.sender_id': 'delete',
  'messages.receiver_id': 'delete',
  'prayers.author_id': 'delete',
  'prayer_amens.user_id': 'delete',
  'user_challenge_completions.user_id': 'delete',
  'membership_account_identities.user_id': 'delete',
  'membership_verifications.user_id': 'delete',
  'membership_identity_reconciliation_requests.user_id': 'delete',
  'sync_events.user_id': 'delete',
  'sync_receipts.user_id': 'delete',
  'privileged_sessions.user_id': 'delete',
  'admin_security.user_id': 'delete',
  'user_permissions.user_id': 'delete',
  'pilot_bootstrap.user_id': 'delete',
  'refresh_tokens.user_id': 'delete',
  // Anonymised: the record stays, the identifying fields are replaced.
  'membership_claims.user_id': 'anonymise',
  // Kept with a reason (append-only history, security, or another person's record).
  'revoked_tokens.user_id': 'keep: revocation list until the token expires (security)',
  'consent_events.user_id': 'keep: append-only consent history',
  'data_subject_requests.requester_id': 'keep: append-only request history',
  'data_subject_request_events.actor_id': 'keep: append-only request history',
  'data_subject_request_revisions.actor_id': 'keep: append-only request history',
  'privacy_notice_acknowledgements.user_id': 'keep: append-only acknowledgement evidence',
  'privacy_presentation_receipts.user_id': 'keep: append-only acknowledgement evidence',
  'privacy_publication_events.actor_id': 'keep: append-only publication history',
  'membership_account_identity_history.user_id': 'keep: append-only identity history',
  'membership_account_identity_history.changed_by': 'keep: append-only identity history',
  'membership_verification_history.user_id': 'keep: append-only verification history',
  'membership_verification_history.actor_id': 'keep: append-only verification history',
  'membership_identity_reconciliation_events.actor_id': 'keep: append-only reconciliation history',
  'membership_identity_reconciliation_revisions.actor_id': 'keep: append-only reconciliation history',
  'membership_import_staging_events.actor_id': 'keep: church register import record',
  'membership_imports.actor_id': 'keep: church register import record',
  'membership_identity_reconciliation_requests.reviewer_id': 'keep: reviewer of another request',
  'admin_audit_log.actor_id': 'keep: append-only administrator audit log',
  'disposal_events.authorized_by': 'keep: append-only disposal record',
  'disposal_events.confirmed_by': 'keep: append-only disposal record',
  'retention_reviews.reviewed_by': 'keep: append-only retention record',
  'community_groups.created_by': 'keep: creator link to the anonymised account',
  'community_classes.created_by': 'keep: creator link to the anonymised account',
  'user_permissions.granted_by': 'keep: records who granted another permission',
  'user_permissions.revoked_by': 'keep: records who revoked another permission',
  'ai_knowledge_conflict_groups.created_by': 'keep: governance record',
  'ai_knowledge_events.actor_id': 'keep: append-only governance record',
  'ai_knowledge_source_reviews.reviewer_id': 'keep: append-only governance record',
  'ai_knowledge_sources.approval_actor_id': 'keep: governance record',
  'ai_knowledge_sources.published_by': 'keep: governance record',
  'ai_knowledge_sources.reviewed_by': 'keep: governance record',
  'ai_knowledge_sources.withdrawn_by': 'keep: governance record',
  'ai_offline_rollback_authorizations.actor_id': 'keep: append-only governance record',
  'ai_scripture_corpora.imported_by': 'keep: governance record',
  'ai_scripture_corpora.published_by': 'keep: governance record',
  'ai_scripture_corpus_events.actor_id': 'keep: append-only governance record'
};


async function eraseMemberData(client, userId, now = Date.now()) {
  if (typeof userId !== 'string' || !userId) throw new Error('eraseMemberData requires a user id');

  // Uploaded media first: collect filenames before the rows disappear.
  const media = await client.query('SELECT filename FROM community_media WHERE owner_id = $1', [userId]);
  const filenames = media.rows.map(row => row.filename);

  // Dependants of the member's posts, then the posts themselves.
  await client.query(`DELETE FROM post_amens WHERE post_id IN (SELECT id FROM community_posts WHERE author_id = $1)`, [userId]);
  await client.query(`DELETE FROM post_comments WHERE post_id IN (SELECT id FROM community_posts WHERE author_id = $1)`, [userId]);
  await client.query(`DELETE FROM event_rsvps WHERE post_id IN (SELECT id FROM community_posts WHERE author_id = $1)`, [userId]);
  await client.query('DELETE FROM community_posts WHERE author_id = $1', [userId]);

  // Prayer requests the member wrote, and amens they gave or received.
  await client.query('DELETE FROM prayer_amens WHERE prayer_id IN (SELECT id FROM prayers WHERE author_id = $1)', [userId]);
  await client.query('DELETE FROM prayers WHERE author_id = $1', [userId]);
  await client.query('DELETE FROM prayer_amens WHERE user_id = $1', [userId]);

  // Comments, amens, RSVPs and memberships the member made on other people's content.
  await client.query('DELETE FROM post_comments WHERE author_id = $1', [userId]);
  await client.query('DELETE FROM post_amens WHERE user_id = $1', [userId]);
  await client.query('DELETE FROM event_rsvps WHERE user_id = $1', [userId]);
  await client.query('DELETE FROM post_reactions WHERE user_id = $1', [userId]);
  await client.query('DELETE FROM comment_reactions WHERE user_id = $1', [userId]);
  await client.query('DELETE FROM event_photos WHERE uploader_id = $1', [userId]);
  await client.query('DELETE FROM group_memberships WHERE user_id = $1', [userId]);

  // Messages, connections and notifications the member sent, received or triggered.
  await client.query('DELETE FROM messages WHERE sender_id = $1 OR receiver_id = $1', [userId]);
  await client.query('DELETE FROM connections WHERE requester_id = $1 OR recipient_id = $1', [userId]);
  await client.query('DELETE FROM in_app_notifications WHERE user_id = $1 OR actor_id = $1', [userId]);

  // Progress and uploads.
  await client.query('DELETE FROM user_challenge_completions WHERE user_id = $1', [userId]);
  await client.query('DELETE FROM community_media WHERE owner_id = $1', [userId]);
  await client.query('DELETE FROM membership_account_identities WHERE user_id = $1', [userId]);

  // Membership claims: keep the decision, remove identifying fields.
  await client.query(
    `UPDATE membership_claims SET user_name = 'Erased member', user_phone = $2 WHERE user_id = $1`,
    [userId, `erased-${crypto.createHash('sha256').update(userId).digest('hex').slice(0, 24)}`]
  );

  // Rows that only exist because of this member.
  for (const table of ['sync_events', 'sync_receipts', 'privileged_sessions', 'admin_security', 'user_permissions', 'membership_verifications', 'membership_identity_reconciliation_requests', 'pilot_bootstrap', 'refresh_tokens']) {
    await client.query(`DELETE FROM ${table} WHERE user_id = $1`, [userId]);
  }

  // Account: anonymise and disable login. Tokens issued before now are rejected.
  const erasedPhone = `erased-${crypto.createHash('sha256').update(userId).digest('hex').slice(0, 24)}`;
  await client.query(
    `UPDATE users SET name = 'Erased member', phone = $2, pin_hash = $3, avatar_url = '',
            is_active = FALSE, auth_valid_after_seconds = $5, updated_at = $4 WHERE id = $1`,
    [userId, erasedPhone, crypto.randomBytes(32).toString('hex'), now, Math.floor(now / 1000) + 1]
  );

  return { filenames };
}

module.exports = { eraseMemberData, ERASURE_POLICY };
