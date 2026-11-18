# Data inventory

Scope: the PostgreSQL server (`server/schema.sql` and migrations 0002–0017) and the Android Room database (`src/main/java/.../data/local/entity/`). Purpose, lawful basis and retention are drafts. Items marked **PENDING** need the owner's decision before publication (plan C.3).

Sensitivity: **P** personal, **S** sensitive (religious belief or health), **C** credential or secret, **O** operational.

## Server tables

| Table(s) | Personal fields | Purpose | Sensitivity | Visible to | Retention |
|---|---|---|---|---|---|
| `users` | `name`, `phone`, `avatar_url`, `campus_id`, `role`, `xp`, `streak`, `grace_points` | Account and progress | P | Name and avatar: members (directory, per the draft notice). Phone: **not verified**; check the directory routes before publication | PENDING |
| `users` (`pin_hash`) | PIN hash (bcrypt) | Sign-in | C | Server only | Until account deletion |
| `revoked_tokens`, `sync_receipts`, `sync_events` | Account identifier, token identifier, sync metadata | Session control and offline sync | O | Server only | PENDING |
| `prayers`, `prayer_amens` | Request text, author, anonymity flag | Prayer requests | S | Per prayer audience; anonymity hides author from other members, not from the service | PENDING |
| `messages`, `connections` | Message text, media reference, participant identifiers | Private messages and connections | P | Participants and administrators under access checks | PENDING |
| `community_posts`, `post_amens`, `post_comments` | Post and comment text, media, author | Community feed | P/S | Per audience | PENDING |
| `community_groups`, `group_memberships`, `event_rsvps`, `community_classes` | Group roster, RSVP, class enrolment | Community activities | P | Group and event members | PENDING |
| `community_media` | Uploaded image metadata and owner | Photo uploads | P | Owner, participants, and post audience (checked on every request) | PENDING |
| `in_app_notifications` | Notification text, recipient | Notifications | P | Recipient | PENDING |
| `daily_challenges`, `user_challenge_completions` | Completion records | Devotional progress | P | Owner | PENDING |
| `game_leaderboard_profiles` (migration 0022) | `opt_in` flag and update time; the first name is taken from `users.name` when shown | Church games leaderboard. Off by default. | P | Only members who have opted in appear, by first name; each member sees their own rank | Until the member opts out or deletes the account (erasure removes the row) |
| `game_score_weeks`, `game_score_events` (migration 0022) | Weekly star totals per member, and one row per uploaded batch (run id, game, points) | Church games leaderboard | P | Totals appear on the board only for opted-in members; the server does not expose run ids | Weekly totals and batch rows are kept until the member deletes the account (erasure removes them) |
| `post_reactions`, `comment_reactions` (draft, migration 0023 in PR 31, not yet on main) | Emoji reaction, user identifier, time | Reactions on posts and comments | P | Members who can see the post or comment | PENDING |
| `event_photos` (draft, migration 0024 in PR 31, not yet on main) | Uploaded photo URL, uploader, review status, reviewer, review time | Member photos on events, shown only after leader approval | P | Post audience once approved; uploader and leaders while pending | PENDING |
| `group_join_requests` (draft, migration 0025 in PR 31, not yet on main) | Requesting member, group, decision and decider | Requests to join closed groups | P | Group leaders; the requesting member | PENDING |
| `community_posts.review_status` (draft, migration 0023 in PR 31, not yet on main) | Review state of a post | Community moderation | O | Per post audience | PENDING |
| `campuses` | Campus name, address, contact | Church information | O | Public | Ongoing |
| `membership_records`, `membership_record_revisions`, `membership_imports`, `membership_import_staging_rows` | Member identity fields from the church register | Membership verification | S | Administrators with membership permissions only | PENDING (import is gated by L8) |
| `membership_claims`, `membership_verifications`, `membership_verification_history`, `membership_claim_rate_limits` | Claim details, review decision, rate-limit counters | Membership claims and review | S | Claimant (receipt) and reviewers | PENDING |
| `membership_account_identities`, `membership_account_identity_history`, `membership_identity_reconciliation_*` | Links between accounts and register records | Identity reconciliation | S | Administrators only | PENDING |
| `privacy_notice_versions`, `privacy_notice_acknowledgements`, `privacy_policy_versions`, `privacy_publication_events`, `privacy_presentation_receipts` | Acknowledgements and version history | Accountability for notices | P | Account owner; administrators | PENDING |
| `consent_purposes`, `consent_events` | Consent choices and time | Consent records | P | Account owner; administrators | PENDING |
| `data_subject_requests`, `data_subject_request_events`, `data_subject_request_revisions` | Request type, status, history | Access, correction and erasure requests | P | Requester; privacy administrators | PENDING |
| `disposal_events`, `retention_rules`, `retention_reviews`, `legal_holds` | Disposal records and holds | Retention enforcement | O | Privacy administrators | Ongoing |
| `admin_audit_log`, `privileged_sessions`, `admin_security`, `permissions`, `user_permissions` | Administrator actions, step-up sessions, TOTP counter (`totp_last_counter`) and secret (`totp_secret_cipher`, encrypted) | Administrative accountability and security | C/O | Administrators | PENDING |
| AI governance tables (migrations 0013–0017 on the local `phase-B-community` line only) | Question and context text as configured, daily budget counters, source approvals | Governed AI (off in release) | P | Administrators | PENDING |

## Android (Room) entities

| Entity (table) | Personal fields | Purpose | Notes |
|---|---|---|---|
| `UserEntity` (`users`) | `name`, `email`, `phone`, `bio`, `avatarUrl`, `campusId`, `role` | Local copy of the signed-in profile | The `email` field is populated from the session payload. The server `users` table has no email column. **PENDING**: confirm whether email is ever stored or displayed, and remove it if not. |
| `PrayerEntity` (`prayers`) | `authorName`, `userId`, `content`, `title`, `isAnonymous` | Offline prayer requests | Synced to server |
| `MessageEntity` (`messages`) | `senderId`, `senderName`, `content`, `mediaUrl` | Offline messages | Synced to server |
| `ConnectionEntity` (`connections`) | `peerName`, `userId`, `peerId` | QR connections | |
| `NotificationEntity`, `CommentEntity`, `GroupEntity`, `GroupMemberEntity`, `EventRsvpEntity`, `ReflectionEntity`, `DailyChallengeEntity` | Community content and progress | Offline community data | |
| Bible games stars and streaks (`SharedPreferences` file `perazim_game_scores_v1`, keys per account) | Stars earned, pending upload batch (run id and points), day streak, best scores | Games progress on this phone; stars upload to the church leaderboard for signed-in members | Keys are a one-way hash of the account id. Not in Room. Removed when the app data is cleared. |
| `SyncQueueEntity` (`sync_queue`) | Payload JSON of pending writes | Offline sync | Deleted when the account is removed from the device |
| `SyncMetadataEntity`, `AppConfigEntity`, `DownloadedContentEntity` | Operational | Sync and configuration | |
| Bible, hymn, sermon, joke, riddle, verse and event entities | None | Church content | Public content |

Session tokens are stored in the Android Keystore-backed store and excluded from backup (`data_extraction_rules.xml`). Audit finding S-01 in `docs/security/audit-2026-11.md` covers the Room database backup rule.

## Branch caveat
This inventory was written against the working tree of `fix/perazim-remediation-start`, which is based on `origin/phase-B-community`. On that base, migration 0013 is `sync_mutation_receipts`; the governed-AI migrations (0013–0017) exist only on the local `phase-B-community` line. The two lines diverge and must be reconciled by the owner before this inventory is final.

## Open items (owner)
- Retention period for every row marked PENDING.
- Lawful basis per purpose (C.3).
- Confirmation of the Room `email` field (above).
