# Runtime reconciliation against Levi's governing docs (25 November 2026)

Read first: LEVI_PROFILE_INDEX.md, then LEVI_THEOLOGICAL_PROFILE.md, BIBLICAL_AUTHORITY_AND_SOURCE_POLICY.md and AUTONOMOUS_EXECUTION_AND_ESCALATION.md.

## Changed in this pass
| Item | Change | Basis |
|---|---|---|
| guide/playbooks/drift.md | Rewritten to 1.3.0: identify the specific teaching from supplied notes, refute from two KJV passages, no blanket decline, no generalising to members, no citing another group's writings as authority | BIBLICAL §3 and §9 (old 1.2.0 said decline any named group and never cite its writings) |
| local/LocalPrompt.java | Added "Always reply in English, even when the question is in Swahili or Sheng." | Profile, communication: English only unless Levi requests otherwise. Swahili and Sheng stay recognised as input for crisis detection only |

Unit tests for org.perazimchurch.app.ai.* pass after these changes.

## Conflicts found, not resolved silently
| # | Conflict | Where | Status |
|---|---|---|---|
| 1 | RESOLVED by Levi (1A, 2026-11-25): full legal name "Perazim Mission Church", short form "PMCI". Applied in the AI prompt and scope line, privacy notices, legal drafts, and the spec documents. Dated identity records are kept as history. Was: Institutional name: "Perazim Mission Church" in app UI, ChurchConfig, SECURITY.md, ProfileUiBinder; "Perazim Missions Church International" (PMCI) in the profile header, privacy notices v0.1/v0.2, AuthDialog, and the AI system prompt and scope line | app code and docs | Done. |
| 2 | RESOLVED by Levi (2A, 2026-11-25): "The Place of Great Breakthrough" everywhere. Onboarding, About and home greeting now show this motto; the "everybody is somebody" slogan is removed from live screens. Was: Motto: "The Place of Great Breakthrough" (2 Samuel 5:20) in OnboardingDialog and ProfileUiBinder; "A place where everybody is somebody, and no body is a nobody." in ProfileUiBinder (about screen). Sheet key church_motto | app code | Done. |
| 3 | Evaluation snapshot (AI_EVALUATION_STATUS_2026-11-25) reports strict 492/886 and decline-pass 521/886 at 3abe4b8. The latest local round 8 on fe95e57 scores strict 509 and decline-pass 537 with stricter expectations. | evaluation records | Report must be regenerated from the tested commit before any gate claim. |
| 4 | Status.md said the test set had 532 cases; the set is 886 (including HS-CESS-001). | guide/Status.md | Fixed in this pass. |
| 5 | Early Christian writings (Ante-Nicene, NPNF) were removed by SOURCE_RESTRICTION, then allowed as Tier 2 by the later policy. Adding them to active answering material needs Levi's approval (AUTONOMOUS §4). | library/sources | Not added. Nicene-Constantinopolitan Creed stays as the approved creed. ESCALATE before adding any Father. |
| 6 | Augustine appears in the apologetics outlines draft. Same escalation as item 5. | ai-eval/drafts/apologetics-outlines-DRAFT.md | On decline. |
| 7 | Gifts-continue sentence and cessationist answer: profile says teach the conviction directly; the exact wording is still pending Levi's sign-off. | ai-eval/drafts/proposed-gifts-continue-DRAFT.md, cessationist-answer-DRAFT.md | Preserved as pending. HS-CESS-001 stays on decline. |
| 8 | Israel and end times round 3: pending Levi's exact-text approval. The zone stays. | ai-eval/drafts/israel-end-times-DRAFT.md | Preserved as pending. |
| 9 | Comparative KB 1.5.22 is restored as false-doctrine material. Its Qur'an paraphrase entries are non-approved sources; the later policy allows narrow use. SOURCE_RESTRICTION still records the Qur'an as removed. | guide/comparative, PerazimAiEngine | Needs Levi confirmation that Qur'an paraphrase stays in the narrow channel. |
| 10 | Swahili and Sheng: CrisisPhrases and routes match Swahili and Sheng input (Levi approved for crisis). The profile says answers are English only. | CrisisPhrases.java | Input-only, now aligned by the prompt clause. |
| 11 | Engine comment described the on-phone model as answering English, Swahili and Sheng. | PerazimAiEngine.java | Corrected to English replies, with Swahili and Sheng as input only. |
| 12 | Engine trigger "swahili bible" and chip "Read Swahili Bible" refer to the Bible reader's Swahili text (Bible reader keeps its translations). Not an AI answer language. | PerazimAiEngine | No change needed; noted. |
| 13 | Divorce, tithing, alcohol, women's ministry, healing, evolution and age of the earth, sexuality, inerrancy wording, final punishment remain UNDECIDED. Routes must say the church has not settled the point. | doctrine register | Checked by DoctrineLibraryTest. Alcohol route still to be checked case by case. |
| 14 | Canon: 66-book canon is DRAFT_PENDING_LEVI. Canon as an entry was removed; it must not be stated as approved. | doctrine register | Consistent. |

## Decision record
- 2026-11-25, Levi: name option 1A ("Perazim Mission Church" full legal name, "PMCI" short form); motto option 2A ("The Place of Great Breakthrough" everywhere). Applied on claude/ai-guide-final-plus-83.

## Levi's 06:30 decisions (2026-11-25), implemented
| Topic | Decision | Where it is implemented |
|---|---|---|
| Israel and end times | Approved subject to three corrections (Rev 13:11–14; Rev 3:10 to Philadelphia; Dan 2:34–35 and 2:44–45). UNDER REVIEW. | ApprovedRoutes "israel-end-times"; zone removed |
| Gifts of the Holy Spirit | Approved. Sentence added to the Holy Spirit entry; Spirit baptism and tongues wording unchanged. | entries.json giftsContinue (rendered by DoctrineLibrary) |
| Cessationism | Approved with refinements. Only when a member asks specifically. | ApprovedRoutes "cessationism"; HS-CESS-001 updated |
| Apostasy and assurance | Revised: assurance (John 10:27–29; Romans 8:38–39), warnings (Hebrews 6:4–6; 10:26–29; 2 Peter 2:20–22), developing view UNDER REVIEW. | ApprovedRoutes "apostasy-assurance" |
| Election, grace and obedience, sovereignty and responsibility | Approved; election UNDER REVIEW. | ApprovedRoutes |
| Identify-and-refute template | Approved with revisions: heading "What this group teaches"; sources as evidence only; teaching refuted, not people; JW adds John 1:3; Nicene sentence removed from the teaching; LDS from Isaiah 43:10, 44:6 and Romans 6:23; purgatory from Hebrews 10:14, 1 John 1:7, Philippians 1:23, 2 Corinthians 5:6–8 as our conclusion; Mary narrowed to "additional mediator of grace". Other groups ask which teaching is meant. | IdentifyRefute (ENABLED); comparative-kb anchors; NoGoZones other-faiths decline |

## Benchmark, round 9 and round 10
- Round 9 (commit d01309b, 886 cases, automatic grader): strict 571/886 (64.4%), decline-pass 594/886, critical 0, safety 62/62, privacy 36/36.
- Round 10: see the run record for the exact commit after the route-pattern fix.
- The gate is not met. Doctrine, reasoning and historical apologetics categories remain below their gates.

## Still to do
- Gated identify-and-refute routes for historical_apologetics and reasoning_critical_analysis (OFF by default), built only from approved wording.
- Full 886-case rerun from the committed branch, then per-category report.
