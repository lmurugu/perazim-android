# DISASTER RECOVERY RUNBOOK — Rebuild Perazim From Zero

**Status:** DESIGN / DRILL SCRIPT. No live backup, restore, key access, production deployment, or member data processing has been performed for this document.
**Prepared:** 2026-11-10. **Baseline:** phase-B-community at cad3758e5469534c01142bbdeadc62371fa4e7f6.
**Applies to:** code, documentation, Android, future iOS, Express, PostgreSQL, app-owned uploads, media catalogue, provider accounts, credentials, and member-device local data.

## 1. Recovery principles

1. **Code != data != identity != hosting.** GitHub contains source/docs; PostgreSQL contains authoritative shared records; private object/volume storage contains app-owned media; signing keys identify app updates; provider accounts control domains, tunnels, notifications, AI and payments.
2. **Cloud sync != backup.** A bad migration, deletion, ransomware, or compromised admin can replicate corruption to a cloud database. Keep encrypted, versioned, independent backups with tested restoration.
3. **3-2-1:** at least three recoverable copies of critical artifacts, on two distinct storage/control surfaces, one off-site/off-account. For high-risk material, add immutable/offline copy. A GitHub copy and a second branch on the same account are NOT independent.
4. **Never restore onto the only surviving copy.** First recover into a new isolated environment, inspect data, then approve cutover.
5. **Never commit secrets, raw member records, donations, pastoral AI prompts, or unredacted screenshots.** Church religious affiliation and prayer content may be sensitive personal data.
6. **No guaranteed restoration of unsynced phone-only data.** A destroyed device may permanently lose pending Room mutations, local-only AI history, and unsaved drafts. Design consented encrypted device export separately if required.

## 2. Recovery inventory — keep a private, encrypted copy off GitHub

| Artifact | Authoritative owner / source | Required independent recovery copy | Restore proof |
|---|---|---|---|
| Git source + history + docs | GitHub repository, approved tags/commit SHAs | Offline encrypted git bundle or mirror on separately controlled storage | Clone from bundle, verify commit SHA and build |
| Android release identity | Original perazim-release.jks + alias/password, app ID | Encrypted escrow with two authorized custodians | Sign a disposable release and verify update signature; NEVER publish keys |
| Android toolchain | build.gradle, Gradle wrapper, SDK/JDK versions | Version manifest, installer checksums, dependency lock/cache where lawful | Clean build |
| PostgreSQL | One production DB, versioned server migrations | Encrypted custom-format logical dumps off-host; optionally WAL for tighter RPO | Restore into disposable DB, verify rows and migrations |
| Server uploads | Docker uploads volume or approved object store | Encrypted object/volume backup plus manifest | Recover a synthetic attachment with access control |
| Temporary community media | Private storage, expires_at metadata | Retain only within approved policy; no accidental indefinite archive | Expiry and access tests |
| Provider-backed sermons | YouTube/Spotify/Facebook or authorized audio host | Back up catalogue metadata, church-owned originals if legally available | Playback/fallback checks; cannot promise external provider bytes |
| Secrets | Private secret manager / offline encrypted vault | Separate key custody, tested emergency access | Restore isolated environment without printing secrets |
| Cloudflare | Account/domain/DNS/tunnel identity | Recovery procedure, DNS inventory, tunnel credentials escrow | New origin, stable hostname, HTTPS, mobile-data reachability |
| FCM/APNs | Firebase project, Apple team credentials | Account recovery procedures, private server credential escrow | Synthetic push to test device |
| Daraja/Paybill | Church-owned merchant account + treasury records | Independent bank/M-Pesa statement and reconciliation export | Reconcile synthetic ledger after DB restore |
| AI | Approved model/knowledge package, Worker source/config, provider account | Model/checksum/license manifest; private provider settings and keys | Offline local response and gated online failure |
| Privacy and governance | Approved notice, permissions, audit retention rules | Versioned approved signed copy outside Git | Verify current version and restore authorization |
| iOS identity (future) | Apple team, bundle ID, certificates/profiles, APNs key | Approved private escrow, Apple account recovery | Clean iOS build and authorized install |
| Phone-only drafts | Android Room / future iOS local SQLite | Optional user-consented encrypted export; not server-authoritative | Explicit device export/import test, never assumed |

**Recovery custodians:** name a primary operator, independent deputy, privacy officer, and finance verifier in an encrypted private contact sheet. Test that no single lost laptop/account/password makes restoration impossible. Record expiry/rotation dates for credentials and app signing identity.

## 3. Backup policy (proposed; owner must approve)

- PostgreSQL: nightly encrypted custom-format dump to an off-host, separately controlled target; verify file checksum and decryption/pg_restore listing after every job. Keep daily/weekly/monthly retention according to church-approved privacy schedule, not an invented universal period.
- Code/docs: encrypted git bundle/mirror after every approved release and at least weekly; keep immutable release manifest with source SHA and artifact SHA-256.
- App-owned uploads: encrypted backup with manifest and access controls. Temporary community media may have 30-day expiry; ensure backup retention and restoration do not silently resurrect expired attachments.
- Infrastructure: encrypted copies of DNS/tunnel config, deployment manifests, environment variable **names** in Git and actual **values** only in secret escrow. Store backup encryption private key separately from encrypted data.
- Test: daily automated integrity report, monthly restore into disposable DB, quarterly full replacement-machine reconstruction, and after every migration or secret rotation.
- Targets: provisional ordinary-data RPO <=24h and RTO <=8h once a replacement host and recovery operators are available; record measured results. Financial correctness requires independent Safaricom/treasury reconciliation, not only database RPO.
- Monitor: failed backup, stale backup, low disk, failed restore, API outage, database growth, expired TLS, storage quota, queue backlog, and unexpected cloud billing. Alerts must not expose member data.

### Example: isolated PostgreSQL encrypted backup (operator review first)

This is an illustrative shell fragment for a **disposable test Compose stack** with age installed. It assumes an AGE_RECIPIENT public key and a writable private backup directory. It does NOT select a production database or run automatically.

~~~bash
set -euo pipefail
: "$AGE_RECIPIENT"    # public encryption recipient; never use a private key here
mkdir -p "$HOME/perazim-test-backups"
OUT="$HOME/perazim-test-backups/perazim-test-$(date -u +%Y%m%dT%H%M%SZ).pgdump.age"
docker compose -f server/docker-compose.yml exec -T db \
  pg_dump -U perazim_user -d perazim_church -Fc \
  | age -r "$AGE_RECIPIENT" -o "$OUT"
sha256sum "$OUT" > "$OUT.sha256"
sha256sum -c "$OUT.sha256"
echo "Encrypted synthetic backup written; restore verification still required."
~~~

**Important:** The example database name comes from server/docker-compose.yml. Run only on a disposable stack. Real deployments must confirm selected host, DB, version, owner, output directory, access controls, and destination. Shell commands are not a substitute for a separately reviewed automated backup script. Avoid plaintext dumps on disk and never echo DATABASE_URL, passwords, JWT, or encryption identities.

### Example: isolated restore drill (NEVER run against the live database)

~~~bash
set -euo pipefail
# On a new disposable Compose stack with the same supported PostgreSQL major version:
# Create a new empty recovery database; DO NOT use the original application DB.
docker compose -f server/docker-compose.yml exec -T db \
  createdb -U perazim_user perazim_restore_test
# BACKUP points to an independently stored encrypted synthetic dump.
# AGE_IDENTITY is the private age identity file, held outside the repository.
age -d -i "$AGE_IDENTITY" "$BACKUP" \
  | docker compose -f server/docker-compose.yml exec -T db \
      pg_restore -U perazim_user -d perazim_restore_test \
      --no-owner --no-privileges --exit-on-error
docker compose -f server/docker-compose.yml exec -T db \
  psql -U perazim_user -d perazim_restore_test -c "SELECT COUNT(*) FROM users;"
~~~

A successful command is **not** a complete restore proof. Verify migration history, FK integrity, user permissions, privacy acknowledgements, token revocation, synthetic giving receipts/audit events (when implemented), uploaded-media references, and replay of pending operations. Run application-level smoke tests against the restored disposable DB; never point a real phone at it without explicit authorization. Protect the age private identity; a public recipient alone cannot decrypt.

## 4. Catastrophic rebuild — exact order of operations

### Step 1 — declare incident and freeze changes

- Record incident ID, UTC time, affected services, last known-good commit, latest backup timestamp, suspected compromise, and decision owner.
- Freeze deployments and production writes where appropriate; preserve logs and snapshots for investigation. If compromise is suspected, use clean devices and rotate credentials before public cutover.
- Protect member privacy and follow the approved Kenyan data-protection incident procedure. Do not publish raw donor/prayer records in a ticket.

### Step 2 — reconstruct repository without relying on GitHub

- On a clean trusted workstation, recover encrypted git bundle/mirror from independent storage. Verify checksum and source commit SHA against offline release manifest.
- Example for an approved, unencrypted *already decrypted* bundle on a clean machine:
~~~bash
git clone /secure/recovery/perazim-all.bundle perazim-recovered
cd perazim-recovered
git rev-parse HEAD
git fsck --full
git status --porcelain
~~~
- If GitHub is available, cross-check remote history and tags, but do not assume GitHub alone is the backup. Re-create a private/approved remote if the account/repository is lost. Restore branch protections, review permissions, CI configuration, and issue/decision history from approved exports.
- Check source/asset licenses, offline Bible translations and model provenance before redistributing artifacts.

### Step 3 — provision clean backend infrastructure

- Choose the owner-approved origin: internal Fedora for testing, or separately approved stable hosted API for member access. **A named Cloudflare Tunnel does not keep a powered-off Fedora machine online.**
- Install pinned OS packages, Node runtime, PostgreSQL 15-compatible server/client, Docker/Compose if using Compose, cloudflared/Caddy as selected, and backup tooling. Verify versions and hashes; avoid pulling unpinned production dependencies without review.
- Prefer the already committed server/docker-compose.yml for a local disposable test stack or server/deploy/compose.yml + Caddy for a separately approved VPS; these are DIFFERENT deployment topologies. The VPS stack is documented as NOT DEPLOYED.
- Provision empty DB, private API environment, firewall, health probes, least-privilege roles, storage and logging. No real member import yet.
- The Fedora server/scripts/local-backend.sh depends on an ignored pre-extracted server/.runtime/pgsql/usr tree. A fresh checkout does not recreate that tree; provision PostgreSQL explicitly rather than assuming the script is portable.

### Step 4 — restore secrets and server identity

- Retrieve private secrets via approved dual-custodian procedure. Restore POSTGRES_PASSWORD, JWT_SECRET, DNS/tunnel credentials, backup keys, provider credentials, and approved AI/FCM/Daraja settings only to the new protected host.
- If keys may have been compromised, rotate and invalidate old tokens/sessions, update secrets with change control, and preserve audit trail. A new JWT secret will invalidate existing sessions; plan member reauthentication.
- Never insert secrets in GitHub Actions logs, public issues, or chat. Avoid storing service-account JSON in the repository.

### Step 5 — restore authoritative PostgreSQL and app-owned media

- Select last clean encrypted DB backup by timestamp and integrity. Restore into a **new** isolated database first. Check migration state against server/migrations/0001–0012 and server/scripts/db-migrate.js; do not rerun an incompatible baseline blindly.
- Run schema/constraint checks, migration preflight, synthetic account login, private-group isolation, sync replay, audit verification and finance reconciliation. Only then authorize cutover.
- Restore app-owned uploads from separate encrypted manifest. Confirm ownership, private access and 30-day media expiry. Restore only unexpired temporary attachments according to policy; backups must not re-publish deleted/expired data.
- For provider-hosted YouTube/Spotify/Facebook content, recover only metadata and lawful church-owned originals. Playback depends on external providers; present fallback links when embeds fail.

### Step 6 — restore DNS, HTTPS, monitoring and public reachability

- For internal testing keep API local/loopback. For a previously approved public service, restore named tunnel identity or authorized reverse proxy, DNS hostname, TLS, origin access policy, rate limits, and health checks.
- Verify from a phone on normal mobile data without Wi-Fi, USB, VPN, adb reverse, or laptop proxy. Do not declare public acceptance from localhost curl alone.
- Verify backup job and independent off-host destination are operational **before** reopening member writes.

### Step 7 — rebuild Android without replacing member data

- Recover exact Android source SHA, JDK 17, Android SDK 34, Gradle wrapper and dependencies.
- Run:
~~~bash
./gradlew testDebugUnitTest assembleDebug --stacktrace
~~~
- For a release, retrieve the original signing keystore and alias/password from escrow. Build using documented signing configuration only in a protected release environment; verify APK signing identity and SHA-256.
- The existing GitHub CI baseline has a known failure at verifyReleaseApk when the expected release APK is missing without signing setup. Fix that through a separately reviewed CI/release design; never weaken the security check to make a green badge.
- Use the isolated .validation app for device testing. Do not uninstall, clear, downgrade, replace the main app, or change account/server endpoint without owner consent.
- A lost release keystore can prevent an APK update over existing installed signatures. Plan authorized key rotation/re-enrollment or store-managed app signing where applicable; this is an identity-recovery problem, not solved by GitHub.

### Step 8 — reconstruct iOS (future deliverable, not present today)

- Restore the approved Swift/SwiftUI source and lockfile/Swift package versions (once built), iOS bundle ID, Apple team/certificates/profiles, APNs key, Xcode/macOS toolchain and test fixtures.
- Build against the same versioned Express API contract; use iOS Keychain for tokens and SQLite local cache. Validate offline Bible, local AI feasibility, queued mutation semantics, privacy, media embeds and notification consent.
- Test on a real iPhone and verify authorized TestFlight/App Store or approved internal distribution. Apple developer account, macOS/Xcode and distribution costs/terms require an explicit operational plan. Android Java source cannot be compiled into a native iOS app.
- If iOS has not been implemented, declare it NOT RECOVERABLE AS A NATIVE CLIENT; documentation alone cannot recreate nonexistent iOS code. A PWA fallback has different capabilities and needs separate approval.

### Step 9 — reconcile finance and notifications

- Compare restored giving events to independently held Safaricom merchant/treasurer statements. Do not assume callbacks lost during outage will replay; reconcile manually or via an authorized provider transaction query.
- Preserve unique provider receipts and append-only correction events. Do not manufacture VERIFIED donations to fill gaps.
- Restore notification records/outbox and FCM/APNs configuration, revoke stale device tokens, suppress sensitive push content, and deduplicate replayed events. FCM may delay/drop messages; app history is authoritative.

### Step 10 — authorize controlled cutover

- Obtain signatures from backend operator, privacy officer, finance verifier, and project owner. Document source SHA, DB backup ID, restore checksum, migrated schema, domain, APK/IPA hash, test results, measured RPO/RTO, residual lost offline drafts, and rollback route.
- Begin with a small controlled cohort; monitor health, database errors, sync backlog, payment reconciliation, privacy complaints, backup freshness, media expiry, and push opt-outs.
- Keep old origin read-only/isolated until cutover is accepted. Revoke old credentials and retire the failed host according to incident procedure.

## 5. Failure-specific playbooks

| Incident | First action | Recovery and proof |
|---|---|---|
| GitHub deleted/account lost | Freeze new releases; use independent git bundle | Verify full history/tags, restore docs/CI, recreate remote; compare source SHA |
| Fedora disk failure/power loss | Offline client mode, queue writes locally | Rebuild on replacement host; restore independent DB/media backup; test replay |
| PostgreSQL corruption/bad migration | Stop writes and preserve forensic snapshot | Restore last clean dump to new DB; run integrity/migration tests; calculate RPO |
| Supabase/cloud account outage | Do not switch to an unapproved second DB | Restore independent export to approved PostgreSQL; re-point Express only after cutover |
| Android phone lost | Revoke sessions/tokens; preserve privacy | Reinstall signed app; fetch server-backed data; warn unsynced drafts/local AI may be lost |
| Android signing key lost | Halt release updates | Investigate approved key rotation/store recovery; do not ship a different signature as a silent update |
| AI Worker/provider outage | Keep online AI fail-closed | Offline approved local guide if available; no queued private prompt auto-send |
| FCM/APNs outage | Keep in-app notification history | Pull on open/refresh; retry outbox; no reliance on push as a data store |
| Daraja callback outage | Keep payments UNVERIFIED/NEEDS_REVIEW | Reconcile provider/treasury statement; dedupe late callbacks |
| Media provider blocks embed | Display graceful fallback | External authorized playback link; no scraping or unauthorized download |
| Tunnel/DNS compromise | Disable exposed route and rotate credentials | Restore stable hostname and access policy on clean origin |
| Backup encryption key lost | Escalate to independent custodian | Restore from approved escrow; if no key, encrypted backup may be unrecoverable |
| Sensitive-data breach | Contain and preserve evidence | Privacy/legal incident procedure, assess notifications, rotate credentials, document impact |

## 6. Recovery acceptance test and evidence template

For every drill record:

- Incident/test ID, date/time UTC, operator, witness, source commit SHA, isolated environment, encrypted backup ID + SHA-256, key custody verification.
- Actual RPO and RTO, number of restored records by category (synthetic counts only in public evidence), migration version, media count/expiry, failed/replayed queue IDs.
- Tests: health endpoint; two synthetic users; denied cross-user/private access; one idempotent prayer/Amen; one duplicate synthetic payment receipt; one denied AI request; one generic notification; server restart; Android .validation app offline/online; iOS equivalent when built.
- Any discrepancy, lost offline-only data, residual exposure, remediation owner, and due date. Store sensitive logs privately, not in the public repo.
- Approval signature and rollback criteria.

**Pass criterion:** A clean team can restore source, secrets, data, infrastructure and signing identity from independently held materials, reproduce builds, restore a disposable DB, and pass the acceptance tests without depending on the destroyed original computer or the original GitHub account. Until a full drill succeeds, mark full recovery readiness **NOT VERIFIED**.

## 7. Further documentation required for production

This runbook is the master plan, not a completed operational inventory. Before production, add protected/private records for: version manifest and SBOM, backup destination and retention, exact tested restore transcript, secret escrow inventory, DNS/tunnel ID and owner, hosting provider and billing, approved privacy/finance policies, provider contracts, signed release manifest, iOS project and Apple identity, OpenAPI spec, migration ledger, runbook owners, and on-call escalation tree. Keep placeholders and public-safe procedures in Git; sensitive values stay outside it.

### References

- [Hybrid checklist](HYBRID_DEPLOY_CHECKLIST.md)
- [Feature contracts](SCHEMA_SYNC_AND_FEATURE_CONTRACTS.md)
- [Existing Room migration evidence](../foundation/ROOM_MIGRATIONS.md)
- [Existing PostgreSQL migration guidance](../../server/docs/POSTGRES_MIGRATIONS.md)
- [Existing deployment preparation](../../server/deploy/README.md)
- [Existing privacy and retention requirements](../PERAZIM_AI_PROJECT/15_DATA_RETENTION_AND_PRIVACY.md)
