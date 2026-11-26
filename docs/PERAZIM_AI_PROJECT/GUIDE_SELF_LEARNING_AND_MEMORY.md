# Perazim Guide: self-learning, self-heal and on-device memory

Status: built on branch `claude/project-thread-3dtcnd` (draft PR). Validation builds only, behind
`OFFLINE_QWEN_ENABLED`. Release builds are unchanged.

## What is locked and what learns

| Layer | Where it lives | Can the phone change it? |
| --- | --- | --- |
| Rules, pipeline, playbooks, skill index, self-learning procedure | `src/main/assets/ai/guide/` (`Agents.md`, `Skills.md`, `Status.md`, `playbooks/`) | No. Shipped in the APK. Changed only by a release that re-runs `ai-eval/perazim-guide/cases.jsonl`. |
| Crisis, privacy and scope rules | Locked code (`PerazimAiEngine`, `OnDeviceAi`) | No. Lessons cannot reach them. |
| Learned style notes (lessons) | Encrypted vault on this device | Yes, only style, length, format and verse-format notes. |
| users.md (name to use, tone, length, reading level, topic counts, feedback counts) | Encrypted vault on this device | Yes, from explicit requests and feedback. |
| User skill (mood and explicit style requests) | Stored as part of users.md, no question text kept | Yes. |

Every install carries its own copy of the locked files, its own vault and its own key. Nothing is uploaded.

## Per answer pipeline (on-phone model)

1. Existing gates run first and are unchanged: crisis, sensitive disclosure, human decision, scope, built-in answers.
2. Retrieval of approved KJV or NASB evidence, as before.
3. `GuideRuntime.contextFor` builds a block of at most 1000 characters: the rules, one playbook chosen by the
   skill index (crisis ranks first), the user's style line, and up to two learned notes. It is added inside the
   system text by `LocalPrompt.buildBounded`.
4. The model answers. Existing citation checks run unchanged.
5. `SelfCheck` heals or refuses before the answer is shown:
   - translation quotes (ESV, NIV, NKJV) are refused;
   - an individual verdict on salvation is replaced with the no-verdict reply;
   - a quotation of four or more words that is not in the supplied evidence is replaced with a note;
   - a chapter outside the book's range is replaced with "(reference to be checked)"; bracketed citations are never changed;
   - a sensitive answer with no referral to a trusted person, the Bishop or a professional gets one added;
   - an answer over 900 characters is cut at a sentence.
6. The outcome is logged as issue codes. Sensitive questions are never logged.

## Learning loop

- Self-check issues become fixed, developer-written style notes (for example "Keep quotes short and copy them exactly.").
- A thumbs-down adds a length or style note and counts a failure against the lessons that were used. A lesson that fails three times is removed.
- A user correction is kept only if it passes `LessonGuard`. Otherwise it is refused and not stored.
- Lessons are capped at 40, deduplicated, and expire after 120 days.

## Lesson guard (what may never be learned)

A lesson is rejected when it contains a restricted topic: doctrine, salvation, sin, grace, baptism, election, safety,
crisis, abuse, medical words, privacy, secrets, scope or refusal, rules or instructions, translations or Bible
versions, authority, money or giving, church positions, or any digit, link or markup. The kinds allowed are STYLE,
LENGTH, FORMAT, VERSE_FORMAT and CHECK only. Unit tests cover these cases.

## Privacy safeguards

- Encrypted at rest with an AES-GCM key in the Android Keystore (the same pattern as the session token store).
  Files are written with a temporary name and then renamed. Damaged files are treated as empty.
- Sensitive questions (crisis, abuse, grief, depression, marriage conflict, confession and similar words) are not
  learned from, not counted in topics and not used for mood.
- Topic counts use the fixed skill ids only, never the words of the question.
- The question text is never stored. Only lessons (style notes), counts and the user's own chosen name are kept.
- "What the guide remembers" shows everything kept. "Forget everything" deletes the personal vault files. Bundled
  operating files are untouched.
- Nothing goes to the network.

## Known limits and decisions for Levi

- The prompt budget was reduced in favour of the guide block. The model may see fewer evidence excerpts. The laptop
  benchmark thread should measure this against `ai-eval/perazim-guide/cases.jsonl`.
- Crisis keyword detection is in locked code and matches English phrases. Swahili phrases are a known gap for a
  code change, not a lesson.
- Self-learning does not retrain the model. It adjusts the style of the prompt only.
- The Android glue (cipher, asset reader, feedback row, memory dialog) could not be compiled in the cloud session
  because there is no Android SDK there. The pure Java core (35 unit tests) compiles and passes. CI is the build check.
- The "What the guide remembers" menu item also appears in release builds. It shows "none yet" there because the guide does
  not run in release. Hide it if Levi prefers.
- Overlap: this branch touches `LocalPrompt.java` and `OnDeviceAi.java`, which the AI benchmark thread also edits on
  `fix/ai-verse-reference`. Rebase needed before merge.

## Files

- `src/main/java/org/perazimchurch/app/ai/guide/` (core, pure Java, plus Android glue)
- `src/main/java/org/perazimchurch/app/presentation/ui/GuideFeedbackRow.java`, `GuideMemoryDialog.java`
- `src/main/assets/ai/guide/` (locked operating files)
- `src/test/java/org/perazimchurch/app/ai/guide/` (unit tests, including the evaluation set validator)
- `ai-eval/perazim-guide/` (532-case JSONL and README)
