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
| `campuses` | Campus name, address, contact | Church information | O | Public | Ongoing |
| `membership_records`, `membership_record_revisions`, `membership_imports`, `membership_import_staging_rows` | Member identity fields from the church register | Membership verification | S | Administrators with membership permissions only | PENDING (import is gated by L8) |
| `membership_claims`, `membership_verifications`, `membership_verification_history`, `membership_claim_rate_limits` | Claim details, review decision, rate-limit counters | Membership claims and review | S | Claimant (receipt) and reviewers | PENDING |
| `membership_account_identities`, `membership_account_identity_history`, `membership_identity_reconciliation_*` | Links between accounts and register records | Identity reconciliation | S | Administrators only | PENDING |
| `privacy_notice_versions`, `privacy_notice_acknowledgements`, `privacy_policy_versions`, `privacy_publication_events`, `privacy_presentation_receipts` | Acknowledgements and version history | Accountability for notices | P | Account owner; administrators | PENDING |
| `consent_purposes`, `consent_events` | Consent choices and time | Consent records | P | Account owner; administrators | PENDING |
| `data_subject_requests`, `data_subject_request_events`, `data_subject_request_revisions` | Request type, status, history | Access, correction and erasure requests | P | Requester; privacy administrators | PENDING |
| `disposal_events`, `retention_rules`, `retention_reviews`, `legal_holds` | Disposal records and holds | Retention enforcement | O | Privacy administrators | Ongoing |
| `admin_audit_log`, `privileged_sessions`, `admin_security`, `permissions`, `user_permissions` | Administrator actions, step-up sessions, TOTP counter (`totp_last_counter`) and secret (`totp_secret_cipher`, encrypted) | Administrative accountability and security | C/O | Administrators | PENDING |
| AI governance tables (migrations 0013–0017, `ai_*` and knowledge tables) | Question and context text as configured, daily budget counters, source approvals | Governed AI (off in release) | P | Administrators | PENDING |

## Android (Room) entities

| Entity (table) | Personal fields | Purpose | Notes |
|---|---|---|---|
| `UserEntity` (`users`) | `name`, `email`, `phone`, `bio`, `avatarUrl`, `campusId`, `role` | Local copy of the signed-in profile | The `email` field is populated from the session payload. The server `users` table has no email column. **PENDING**: confirm whether email is ever stored or displayed, and remove it if not. |
| `PrayerEntity` (`prayers`) | `authorName`, `userId`, `content`, `title`, `isAnonymous` | Offline prayer requests | Synced to server |
| `MessageEntity` (`messages`) | `senderId`, `senderName`, `content`, `mediaUrl` | Offline messages | Synced to server |
| `ConnectionEntity` (`connections`) | `peerName`, `userId`, `peerId` | QR connections | |
| `NotificationEntity`, `CommentEntity`, `GroupEntity`, `GroupMemberEntity`, `EventRsvpEntity`, `ReflectionEntity`, `DailyChallengeEntity` | Community content and progress | Offline community data | |
| `SyncQueueEntity` (`sync_queue`) | Payload JSON of pending writes | Offline sync | Deleted when the account is removed from the device |
| `SyncMetadataEntity`, `AppConfigEntity`, `DownloadedContentEntity` | Operational | Sync and configuration | |
| Bible, hymn, sermon, joke, riddle, verse and event entities | None | Church content | Public content |

Session tokens are stored in the Android Keystore-backed store and excluded from backup (`data_extraction_rules.xml`). Audit finding S-01 in `docs/security/audit-2026-11.md` covers the Room database backup rule.

## Open items (owner)
- Retention period for every row marked PENDING.
- Lawful basis per purpose (C.3).
- Confirmation of the Room `email` field (above).
