# G04 AI User and Role Permissions Policy

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by church leadership and the product owner
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Principles

1. **Server-side enforcement.** Every role and permission is checked by the backend. The model must never grant or widen a permission.
2. **Claims do not grant access.** A user saying "I am a pastor" grants nothing. Only an assigned role in the system does.
3. **Separate, auditable roles.** One person may hold several roles, but each permission is defined and logged separately.
4. **Technical access is not theological authority.** A system administrator cannot approve doctrine or read pastoral conversations by virtue of administering the system.

## 2. Roles

| Role | Who | Summary of access |
|---|---|---|
| Guest | Unauthenticated or non-member | Approved public information, general faith questions, public Bible resources. No confidential church records |
| Registered adult member | Adult member, verified at registration | Full permitted AI conversation, Bible study help, devotional material, study outlines, approved membership functions |
| Verified ministry leader | Member whose leadership role is assigned and verified | Member permissions plus full sermon drafts, preaching outlines and ministry preparation resources |
| Pastoral care personnel | Named by church leadership | Pastoral referral and support functions. **No automatic access** to private AI conversations |
| Church administrator | Named by church leadership | Approved administrative functions and operational information within assigned scope |
| System administrator | Technical staff | Infrastructure management. No theological authority, no unrestricted pastoral-data access |
| Theological approver | Designated by the church | Review, approve and supersede doctrinal material through the approval workflow |

## 3. Permission matrix

| Capability | Guest | Member | Verified leader | Pastoral | Admin | Sys admin | Approver |
|---|---|---|---|---|---|---|---|
| General faith questions and Scripture study | Yes | Yes | Yes | Yes | Yes | No | Yes |
| Study outlines, devotionals, discussion questions | Limited | Yes | Yes | Yes | Yes | No | Yes |
| Full sermon draft or manuscript | No | No | Yes | Yes | No | No | No |
| Read other users' AI conversations | No | No | No | **No** | No | No | No |
| Official church information (G06) | Public only | Yes | Yes | Yes | Yes (scope) | No | Yes |
| Approve doctrine | No | No | No | No | No | No | Yes |
| Change system configuration | No | No | No | No | No | Yes | No |

Notes:

- "Verified leader" is **not yet defined**. The definition and verification procedure are a church action item (CHK item 4). Until then, no account receives sermon-drafting access.
- AI-generated sermons are not official church teaching until a human ministry leader approves them (G03 and G07).

## 4. Built-app baseline

The current app has **no role or permission system** in code. Implementing G04 is a **requirement to implement**. It is an addition and does not contradict the built app.

## 5. Age controls

- Adults only for the AI Guide (G03 section 7).
- The minimum age is an owner decision reserved in `AGENTS.md` and is not set here.
- Age verification and separate accounts are required before any future under-18 access. A parent sharing a login does not qualify.

## 6. Auditing

- Each permission grant and revocation is logged with who made it and when.
- Access to any conversation by an administrator is logged and only permitted under a documented, approved process.

## 7. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft |
