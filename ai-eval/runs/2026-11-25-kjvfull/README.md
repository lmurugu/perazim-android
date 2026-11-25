# Run on commit 409a304 (branch claude/ai-guide-final), 2026-11-25: full-KJV verse route

Same harness and grader as ai-eval/runs/2026-11-25-c18. What changed: the verse route reads the full bundled KJV
(assets/bible/text/KJV.tsv.gz, 31,102 verses) instead of the 179-verse starter file, and says when a requested
translation (ESV, NIV, NKJV and others) is not one the app holds.

Results (strict, automatic checks only):
- 682 set: 250/682 (36.7%). Previous run (c18d8f7, starter file): 253/682 (37.1%).
- Kenya dev 40/102 (39.2%). Kenya blind 43/101 (42.6%). Reported separately.
- Safety 36/36 on 682, 13/13 Kenya dev, 13/13 Kenya blind. Privacy 30/30, 3/3, 3/3.

Why the 682 number went down: the full KJV now quotes verses that the cases expect the guide to say it cannot verify
(for example FW-080 Matthew 17:25, FW-082 1 Corinthians 13:4-7, VC-082, VT-206, EX-111 John 3:16 with an ESV request).
Quoting the KJV verbatim is the designed behaviour; the case expectations ("ADMIT_UNCERTAIN") conflict with it.
Those expectations need Levi's review. Not tuned here.

Knowledge package: not connected. The product retrieval (OfflineKnowledgeStore.retrieve) reads a signed package.
The package in the repo is a synthetic validation package, and the JVM test provider cannot satisfy the crypto check
(the reason this harness skips it). The approver signing keys are held offline by Levi. See the coordinator report.
