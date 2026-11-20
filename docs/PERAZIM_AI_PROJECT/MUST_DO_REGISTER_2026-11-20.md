# Must-do register (2026-11-20)

Reconciled against: the owner's 2026-11-20 instruction, `docs/internal/remediation-plan.md`, `AGENTS.md`, and the AI project documents. Status words: Implemented (code present), Tested (automated), Demonstrated (runtime evidence), Approved (owner).

| ID | Item | Status | Evidence | Remaining work | Blockers / dependencies | Owner approval needed? |
|---|---|---|---|---|---|---|
| A1 | Document 02 authoritative for identity, boundaries, communication | Implemented, Tested (partly) | `LocalPrompt.java` (d1735c5, 661ca7a); `AuthorityBoundaryTest`; Doc 02 section 21 | Prove the full pathway (see A2) | None | Approved as instructions; re-confirm final text |
| A2 | Instructions reach the actual model; no secondary prompt overrides | Partly verified | Code read: `OnDeviceAi` passes `LocalPrompt.build` output via `-p` | Runtime check with the model's chat template on a device | Phone runtime (F) | Runtime approval |
| A3 | No conversation/retrieved content gains authority | Partly tested | `AuthorityBoundaryTest` injection cases | Retrieved-document injection test (E) | Knowledge package (B) | No |
| A4 | Output validation does not reject legitimate answers | Not tested | None | Build a legitimate-answer test set | Runtime answers | No |
| B1 | Knowledge-package inventory | Implemented (this dossier) | `KNOWLEDGE_PACKAGE_DOSSIER_2026-11-20.md` | Owner review | Owner | Yes |
| B2 | Conflicts (church name, motto, mission, vision, values) | Identified | Dossier section 2 | Leadership decision | Owner | Yes |
| B3 | Knowledge package approval | Not approved | None | After B1–B2 | Owner | Yes |
| C1 | Qwen loads via offline pathway | Not demonstrated | Model SHA check in code; no device run | Phone run | Install approval | Yes (install) |
| C2 | Inference works with no internet | Not demonstrated | None | Airplane-mode phone run | Install approval | Yes |
| C3 | Graceful model-loading failure | Code present, Not demonstrated | `OnDeviceAi` throws IOException → callback | Device test | Install approval | Yes |
| C4 | Fails safely, no fabricated answer | Partly tested | Evidence required before generation; citation check | Device test | Install approval | Yes |
| C5 | Online AI disabled unless authorised | Verified by build config | `ONLINE_AI_ENABLED=false` in all variants (`build.gradle`) | Re-check at each build | None | Yes, to enable |
| C6 | Resource fit for target devices | Not measured | Model 491 MB; 2048-token context; 3 threads | Device RAM and latency measurements | Install approval | Yes |
| D1 | Impersonation, invented positions, overrides, member decisions, app-action claims, privileged extraction | Partly tested | `AuthorityBoundaryTest`, `OfflineScopeHarnessTest` | Runtime adversarial suite | Phone runtime | Runtime approval |
| D2 | Injection in retrieved and user documents | Not tested | None | Test set and runtime run | Knowledge package | Yes (knowledge) |
| D3 | Output-validation effectiveness and false rejection | Partly tested | Pattern tests only | Legitimate-answer set | Runtime | Runtime approval |
| E1 | Architecture and boundaries preserved | Implemented | No Android stack change; ONLINE off; no cloud dependency added | Keep checking | None | No |
| F1 | Phone runtime-testing plan | Written | `RUNTIME_TEST_PLAN_2026-11-20.md` (not executed) | Owner approval to run | Owner | Yes |
| G1 | Remediation plan items (Phases 0–6, Security, Compliance) | Per plan | `docs/internal/remediation-plan.md` (updated 2026-11-18) | Not part of this offline-AI thread | Owner | Owner decisions D1–D4, church name, legal |
| G2 | Launch gates L4, L7, L8 (offline/online AI and data gates) | None ticked | Plan status table | Owner-level gates | Owner | Yes |
| G3 | Minimum age 18+ | Decided and implemented | Owner decision 2026-11-18 | None | None | No |

## Update 2026-11-20 (evening; owner direction of 19:29)

| ID | Update | Status now | Evidence | Remaining |
|---|---|---|---|---|
| B1 | Source approval matrix and missing-documents register | Written | `KNOWLEDGE_SOURCE_APPROVAL_MATRIX_2026-11-20.md`, `MISSING_DOCUMENTS_REGISTER_2026-11-20.md` | Owner decisions per row |
| B2 | Identity comparison: name, motto, slogan, vision, mission (2 versions), values | Written; conflicts open | `CHURCH_IDENTITY_COMPARISON_2026-11-20.md` | Leadership decision. Owner has confirmed the AI-facing name only |
| B4 | Content-rights register (Bible, hymns, sermons) | Written; nothing cleared | `CONTENT_RIGHTS_REGISTER_2026-11-20.md` | Owner permissions |
| A3 | Synthetic retrieved-document injection tests | Automated (host) | `SyntheticInjectionAndValidatorTest.injectedRetrievedTextStaysInsideUntrustedBlock` | Real-model run (F) |
| D3 | Output validator: prohibited and legitimate fixtures both tested | Automated (host); legitimate replies pass, prohibited replies rejected | `SyntheticInjectionAndValidatorTest` | Real-model replies |
| A4 | Output validation false rejection | Partly tested (fixture set of 10 legitimate replies) | same | Real-model replies |
| C1–C6 | Offline verification | Still not demonstrated on a device | See verification report | Phone runtime (owner authorisation) |
| F1 | Runtime plan | Written, not executed | `RUNTIME_TEST_PLAN_2026-11-20.md` | Owner authorisation |

Dependency added: `testImplementation 'org.json:json:20240303'` (host tests only, `build.gradle`).

Completion states used: Implemented, Automatically tested, Manually verified, Runtime verified, Formally approved, Blocked, Awaiting owner decision. No item is marked Runtime verified or Formally approved except Document 02 as instructions.
