# Autonomous Execution, Decision Boundaries and Escalation

**Owner:** Rev. Levi  
**Date:** 25 November 2026  
**Applies to:** Claude, code agents, research agents and contributors working on Perazim AI.

## 1. Purpose

These rules prevent repetitive clarification rounds. Agents should use Levi's recorded positions to carry out ordinary work independently. They must not ask Levi to repeat decisions already documented or choose between options when his instructions establish the answer.

Autonomy means completing authorised work responsibly. It does not transfer Levi's theological authority, institutional authority or release authority to an agent.

## 2. Read before acting

Before changing doctrine, biblical answers, AI sources, retrieval, guide rules, test expectations or church-facing language, read:

- LEVI_PROFILE_INDEX.md
- LEVI_THEOLOGICAL_PROFILE.md
- BIBLICAL_AUTHORITY_AND_SOURCE_POLICY.md
- ISRAEL_CHURCH_AND_END_TIMES_POSITION.md where relevant
- the live doctrine register and approved entries
- the actual current source code, source register, prompts, playbooks and tests being changed.

Always verify the current branch and working tree before editing. These documents guide decisions; they do not replace inspection of the current code.

## 3. Decisions Claude may make independently

When the intended result is clear from Levi's records, Claude may proceed without asking:

- Correct grammar, spelling, structure and readability without changing doctrine.
- Rewrite member-facing explanations in everyday language while preserving approved meaning.
- Add relevant Bible passages that genuinely support an established position, after checking context and exact text.
- Replace isolated proof-texting with multiple contextual passages.
- Remove neutral comparisons that conflict with the Bible-first, Protestant, Pentecostal and Evangelical teaching policy.
- Give the approved Bible-centred Pentecostal answer directly when a user has not asked about alternatives.
- When specifically asked about cessationism, explain the intra-Protestant disagreement and assess it from Scripture without labelling individual cessationists heretics or unsaved.
- Use creeds and early Christian writings only after checking their particular claims against Scripture.
- Use approved Protestant, Pentecostal and Evangelical sources as subordinate support.
- Consult another group's own source narrowly to verify its teaching, then use Scripture to refute the contradiction. Never use that source as an authority for Christian doctrine.
- Update tests to reflect a clear, later instruction from Levi. Preserve coverage; never weaken or delete meaningful tests merely to improve scores.
- Fix code defects within authorised scope, preserve unrelated work, run available tests and report limitations honestly.
- Align source metadata, retrieval, prompts, playbooks, documentation and tests so that they implement the same hierarchy.
- Mark unsupported claims as unverified, remove them or state that the evidence is insufficient rather than inventing an answer.

Do not forward trivial wording choices to Levi. Choose the clearest wording consistent with the documented position.

## 4. Matters requiring escalation

Escalate only when an issue is genuinely unresolved and materially affects doctrine, source eligibility, safety, privacy or owner authority.

Levi's decision is required before:

- Changing an UNDER_REVIEW or UNDECIDED doctrine to APPROVED.
- Inventing or selecting a Perazim position on an undecided topic.
- Changing the theological meaning of an approved entry.
- Resolving a genuine conflict between clear, current instructions where date, explicitness and scope do not settle it.
- Adding a new creed, Church Father, confession or specialist theological source to active answering material where its eligibility or alignment has not been established.
- Making a material doctrinal claim based on a primary source that could not be accessed or checked.
- Establishing official church identity, leadership, governance, membership, discipline or finance policy from conflicting records. In particular, older records and local app documentation contain inconsistent Perazim institutional names and motto/mission wording; do not silently combine them or select one as official without the authorised leadership decision.
- Giving final approval to a member-facing doctrinal answer where Levi explicitly reserved review.
- Removing or bypassing safety, safeguarding, privacy, crisis or abuse protections.
- Merging PRs, merging into main, releasing production builds, enabling production model flags, activating production retrieval or transferring content to a live model.
- Making a consequential change to personal data, member privacy, account access or security.

When escalating, ask one focused question. State the exact unresolved decision, what has already been established, why it matters, and the safest recommendation. Do not send Levi a long menu of options unless genuinely necessary.

## 5. Specific current boundaries

### Israel and the end times

The underlying end-times theology is UNDER REVIEW. Levi approved the third revised member-facing answer on 25 November subject to three specific corrections. The coordinator reports those corrections implemented, the route active in the AI branch and the no-go zone lifted. Preserve the distinction: the corrected answer route is approved for AI-branch evaluation; the detailed theological framework remains under review and is not thereby settled doctrine.

A message saying "the draft is ready" is not approval. An evaluation case passing or failing is not theological approval. If source verification or regression tests show that the served answer differs from Levi's approved text, correct it and report the difference. Production release remains a separate owner decision.

### Holy Spirit and cessationism

Teach the continuing operation of spiritual gifts as Levi's stated position. Do not routinely add neutral competing-view blocks. The 25 November evaluation report says the exact proposed gifts-continue sentence still awaits Levi's sign-off; preserve that distinction.

A specific cessationist question can be answered from Scripture. Do not label cessationists as heretics or unsaved. If a particular wording has been reserved for Levi's approval, keep that wording pending while improving the rest of the answer and route.

### Round 11 Scripture outlines

Follow [LEVI_REVIEW_DECISIONS_2026-11-25_ROUND11_SCRIPTURE_OUTLINES.md](LEVI_REVIEW_DECISIONS_2026-11-25_ROUND11_SCRIPTURE_OUTLINES.md). Do not ask Levi to repeat these decisions. The believer-possession question remains UNDECIDED; prophecy continues as a gift but individual claims and any claimed prophetic office require discernment; Christians should pray for the sick without teaching that faith guarantees healing, while healing in the atonement remains UNDECIDED; do not invent an exhaustive list of essential doctrines; the formal inerrancy formulation remains UNDECIDED while Scripture retains its supreme authority. The problem-of-evil outline is an apologetic explanation, not an UNDER REVIEW doctrine.

### Source policy

The newer source policy supersedes older blanket-removal or blanket-decline rules. Another group's official source may be consulted only to identify its own claim; it is never a Christian doctrinal authority. Creeds and patristic writings may be used only after biblical review.

### Evaluation and release

Never interpret a high benchmark score as authorisation to release. Report strict and decline-pass results separately, preserve critical and safety checks, and identify the actual tested commit. Safety and privacy passing does not mean doctrine, accuracy, impersonation, or all other categories have passed.

## 6. Project execution mandate

Within the authorised scope, agents are expected to:

- Independently implement and fix the assigned work.
- Investigate and repair related defects discovered during implementation when they are within scope.
- Preserve existing work and avoid overwriting unrelated changes.
- Test changes with the strongest available checks.
- Verify the resulting branch, commit, test output and file changes.
- Record what passed, failed, was skipped or could not be run.
- Avoid asking for approval of routine implementation decisions already established by this policy.

Escalate only genuine approval boundaries, consequential decisions or unavoidable blockers. Do not promise work later or claim completion without evidence.
