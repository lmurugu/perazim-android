# Final release-blocker remediation 02

**Build posture:** Internal validation artifact only. No public Release Candidate is claimed.

## Online AI and Qwen

The Android relay client no longer opens a network connection, packages a relay URL or sends a shared app token. No authenticated Express AI endpoint with a verified current permission contract, and no Worker source/configuration, was available in this checkout. Online AI is therefore disabled in every Android variant until an authenticated server-side path is implemented and verified. The UI says it is unavailable and does not silently fall back to Qwen.

Qwen receives unrestricted user prompts today; there is no approved knowledge-package catalogue, integrity/withdrawal/version check or deterministic retrieval boundary. A prompt-only instruction cannot provide that boundary. Qwen inference and model download are disabled in every variant until approved-source retrieval and output controls exist. Built-in deterministic app help remains available and is not represented as generative or source-grounded AI.

The previously embedded relay credential must be treated as potentially exposed. This change removes the Android use and packaging path, but no evidence establishes whether the credential remains active or has been rotated. The credential owner must revoke/rotate it at its provider, update only server-side secret storage after the authenticated relay is implemented, and verify the old credential is rejected. No credential value is recorded here.

## Spreadsheet feed assessment

The eight build inputs previously populated were all HTTPS spreadsheet-host URLs. A redacted local parser found no URL userinfo or query-key/value shape that signalled an embedded credential. The values, IDs, hostnames, paths and query values are intentionally omitted. Because source ownership/ACL, publication status and actual feed contents were not verified, none is classified as an approved public feed.

| Configuration key | Purpose | Exposure classification | Personal/restricted data exposure | Included in replacement APK? | Treatment |
|---|---|---|---|---|---|
| `SHEET_URL_ANNOUNCEMENTS` | Church and app announcements | UNRESOLVED | Feed contents and ACL unverified; personal data cannot be ruled out | No | Remote sync disabled; local content remains |
| `SHEET_URL_EVENTS` | Event listings | UNRESOLVED | Feed contents and ACL unverified; attendee/member data cannot be ruled out | No | Remote sync disabled; local content remains |
| `SHEET_URL_HYMNS` | Hymn catalog | UNRESOLVED | Feed contents and ACL unverified | No | Remote sync disabled; local content remains |
| `SHEET_URL_RIDDLES` | Riddle content | UNRESOLVED | Feed contents and ACL unverified | No | Remote sync disabled; local content remains |
| `SHEET_URL_JOKES` | Joke content | UNRESOLVED | Feed contents and ACL unverified | No | Remote sync disabled; local content remains |
| `SHEET_URL_SERMONS` | Sermon catalog | UNRESOLVED | Feed contents and ACL unverified; pastoral/private references cannot be ruled out | No | Remote sync disabled; local content remains |
| `SHEET_URL_VERSES` | Bible and daily verse content | UNRESOLVED | Feed contents and ACL unverified | No | Remote sync disabled; local content remains |
| `SHEET_URL_CONFIG` | Application configuration | UNRESOLVED | Could expose or control sensitive runtime settings; contents and ACL unverified | No | Remote sync disabled; local content remains |

Build-time loading of these eight fields was removed. The release UI reports that online content feeds are unavailable in this build and local content is being used. Re-enable a feed only after its publication authority, access control and contents are reviewed; private feeds require a server-mediated contract.

## Privacy and registration

Server notice v0.1 remains the database's draft registration notice; v0.2 remains a working draft. Privacy endpoints remain fail-closed and registration still requires the server's current published notice version and content hash. Neither document was published or marked approved. Android's stored acknowledgement text and the server's stored text must be re-compared with a final institutionally approved version before registration is enabled. The review checklist is in `docs/data-protection/INSTITUTIONAL_REVIEW_CHECKLIST.md`.

## Remaining operational decisions

- The church must review and authorize each public feed; URLs are not restored by this change.
- The Worker owner must provide source/configuration and an authenticated relay contract that uses the current server identity and permission model; no AI permission name is inferred.
- Counsel and PMCI reviewers must resolve the controller/contact, lawful basis, providers/transfers, age/guardian approach, rights operations, retention/backups and legal references before notice publication.
- A production administrator provisioning tool and controlled execution environment were not available. The synthetic bootstrap remains test-only; no real administrator was activated. See `docs/phase-b/ADMIN_PROVISIONING_PROCEDURE.md`.
