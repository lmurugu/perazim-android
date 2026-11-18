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

  // Account: anonymise and disable login.
  const erasedPhone = `erased-${crypto.createHash('sha256').update(userId).digest('hex').slice(0, 24)}`;
  await client.query(
    `UPDATE users SET name = 'Erased member', phone = $2, pin_hash = $3, avatar_url = '',
            is_active = FALSE, updated_at = $4 WHERE id = $1`,
    [userId, erasedPhone, crypto.randomBytes(32).toString('hex'), now]
  );

  return { filenames };
}

module.exports = { eraseMemberData };
