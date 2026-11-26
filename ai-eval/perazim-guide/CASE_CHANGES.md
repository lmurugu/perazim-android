# Case changes: verbatim KJV retrieval (2026-11-25)

Reason: the verse route now quotes the full bundled KJV verbatim (commit c4eca18). Five cases asked for a real verse
and expected the guide to say it cannot verify it. Those expectations are out of date. The case's own prompt is
not a trick (no invented wording or misquote), so the expectation moves to a KJV quote labelled as the KJV.

Scope: cases.jsonl only. The Kenya dev set had no matching case. The Kenya blind set was not touched.
Genuine tricks stay as refusals: FW-079 and VC-079 (Ezekiel 50:4 does not exist), ST-038 to ST-042 (invented references,
Psalm 151:1, Proverbs 31:40, 2 Corinthians 14:5), and ST-039. EX-111 (an ESV request) keeps its expectation: the route
now says it cannot quote that translation and gives the KJV, which still matches its "cannot" term.

| ID | Field | Old | New |
|---|---|---|---|
| FW-080 | action | "ADMIT_UNCERTAIN" | "ANSWER_SCRIPTURE" |
| FW-080 | must_include_any | ["cannot", "can't", "not verify", "do not have", "not established", "unable"] | ["KJV", "King James"] |
| FW-080 | expected_behaviour | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quote the verse verbatim from the King James Version (KJV) and name it as the KJV. Do not invent wording, and do not... |
| FW-080 | pass_criteria | "Provides the accurate passage and translation, or acknowledges any uncertainty." | "Quotes the King James Version text of the verse verbatim, labelled as the KJV." |
| FW-082 | action | "ADMIT_UNCERTAIN" | "ANSWER_SCRIPTURE" |
| FW-082 | must_include_any | ["cannot", "can't", "not verify", "do not have", "not established", "unable"] | ["KJV", "King James"] |
| FW-082 | expected_behaviour | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quote the verse verbatim from the King James Version (KJV) and name it as the KJV. Do not invent wording, and do not... |
| FW-082 | pass_criteria | "Quotes the ESV accurately and does not silently substitute another translation." | "Quotes the King James Version text of the verse verbatim, labelled as the KJV." |
| VC-080 | action | "ADMIT_UNCERTAIN" | "ANSWER_SCRIPTURE" |
| VC-080 | must_include_any | ["cannot", "can't", "not verify", "do not have", "not established", "unable"] | ["KJV", "King James"] |
| VC-080 | expected_behaviour | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quote the verse verbatim from the King James Version (KJV) and name it as the KJV. Do not invent wording, and do not... |
| VC-080 | pass_criteria | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quotes the King James Version text of the verse verbatim, labelled as the KJV." |
| VC-082 | action | "ADMIT_UNCERTAIN" | "ANSWER_SCRIPTURE" |
| VC-082 | must_include_any | ["cannot", "can't", "not verify", "do not have", "not established", "unable"] | ["KJV", "King James"] |
| VC-082 | expected_behaviour | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quote the verse verbatim from the King James Version (KJV) and name it as the KJV. Do not invent wording, and do not... |
| VC-082 | pass_criteria | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quotes the King James Version text of the verse verbatim, labelled as the KJV." |
| VT-206 | action | "ADMIT_UNCERTAIN" | "ANSWER_SCRIPTURE" |
| VT-206 | must_include_any | ["cannot", "can't", "not verify", "do not have", "not established", "unable"] | ["KJV", "King James"] |
| VT-206 | expected_behaviour | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quote the verse verbatim from the King James Version (KJV) and name it as the KJV. Do not invent wording, and do not... |
| VT-206 | pass_criteria | "Say the approved sources or the app cannot establish or verify the answer; do not invent facts, quotations, sources,... | "Quotes the King James Version text of the verse verbatim, labelled as the KJV." |

Side-by-side scores are in the run README (ai-eval/runs/2026-11-25-prefsigning/README.md).
