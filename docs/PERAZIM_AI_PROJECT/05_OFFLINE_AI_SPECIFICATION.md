# PERAZIM MISSIONS CHURCH INTERNATIONAL
# AI PROJECT

## Document 05: Offline AI Specification

**Document number:** 05  
**Document status:** Technical and functional specification  
**Project:** Perazim Missions Church International AI Assistant  
**Primary model:** Existing Qwen2.5 0.5B Instruct GGUF  
**Inference runtime:** Existing packaged ARM64 llama.cpp implementation  
**Local storage:** Android Room / SQLite  
**Connectivity requirement:** Must operate without internet access  
**Related documents:**
- `00_PROJECT_OVERVIEW.md`
- `01_TECHNICAL_ARCHITECTURE.md`
- `02_AI_IDENTITY_AND_BEHAVIOUR.md`
- `03_THEOLOGICAL_FOUNDATION.md`
- `04_KNOWLEDGE_ARCHITECTURE.md`
- `06_ONLINE_AI_SPECIFICATION.md`
- `07_RESPONSE_ROUTING_AND_CACHING.md`
- `08_SECURITY_AND_ACCESS_CONTROL.md`

---

# 1. PURPOSE

The offline AI is the locally operating AI assistant within the Perazim Android application.

Its primary purpose is to provide useful Christian, biblical, institutional and ministry assistance without requiring an internet connection.

It must continue functioning when:

- The user has no mobile data.
- The user is outside network coverage.
- The user is travelling.
- The user cannot afford internet access.
- The online AI service is unavailable.
- The device is operating in airplane mode.

Offline access is a central product capability, not merely a fallback for failed online requests.

The offline assistant must provide the best possible answers within the capabilities of the locally installed model and approved local knowledge.

# 2. EXISTING IMPLEMENTATION TO PRESERVE

Codex must build on the existing implementation.

The inspected application currently includes:

- Qwen2.5 0.5B Instruct in GGUF format.
- Quantised Q4_K_M model.
- Packaged ARM64 llama.cpp runtime.
- Existing `OnDeviceAi.java` integration.
- Existing local prompt construction through `LocalPrompt.java`.
- Existing `PerazimAiEngine.java` programmed response rules.
- Existing Room database and SQLite storage.
- Existing local Bible retrieval through `BibleContextExtractor.java`.

These components must be inspected before modification.

Codex must not replace the existing model, runtime or architecture simply because a different implementation is easier to introduce.

Changes must preserve the existing Android application and its unrelated features.

# 3. OFFLINE OPERATING REQUIREMENTS

The offline assistant must:

1. Start without an internet connection.
2. Answer using locally available approved knowledge.
3. Retrieve relevant Scripture from the local Bible database where licensing permits.
4. Retrieve relevant church documents from the approved local knowledge package.
5. Apply the same theological authority rules as online AI.
6. Respect local access permissions and package restrictions.
7. Avoid sending private information to external services.
8. Clearly disclose when its knowledge is incomplete or outdated.
9. Handle model failures without crashing the application.
10. Operate within reasonable Android memory, storage and battery constraints.

The offline assistant must not pretend to have access to current internet information.

# 4. OFFLINE REQUEST ORCHESTRATION

Every offline question must pass through a common orchestration process.

The process should be:

```text
USER QUESTION
      |
INPUT VALIDATION
      |
USER ACCESS AND CONTENT PERMISSIONS
      |
SCOPE AND SAFETY CLASSIFICATION
      |
REQUEST CLASSIFICATION
      |
DETERMINISTIC RESPONSE CHECK
      |
APPROVED LOCAL CACHE CHECK
      |
LOCAL KNOWLEDGE RETRIEVAL
      |
SCRIPTURE RETRIEVAL WHERE RELEVANT
      |
QWEN PROMPT CONSTRUCTION
      |
LOCAL MODEL GENERATION
      |
RESPONSE VALIDATION
      |
ANSWER + SOURCES + LIMITATIONS
```

The implementation may reorder or optimise individual stages where necessary, but safety, scope and access checks must happen before retrieval or generation.

The local model must not be allowed to bypass these checks.

# 5. REQUEST CLASSIFICATION

The offline assistant should classify questions into appropriate categories.

At minimum:

- Perazim church information.
- Official doctrine.
- Bible interpretation.
- Bible references.
- Christian apologetics.
- Christian discipleship.
- Prayer and spiritual growth.
- Worship and ministry.
- Approved ministry programmes.
- Application assistance.
- Pastoral support.
- Sensitive pastoral concern.
- Emergency or crisis concern.
- Outside-scope question.

Classification must be implemented using deterministic rules, lightweight local logic or the existing model where appropriate.

The system must not rely entirely on the model to recognise sensitive content or enforce access restrictions.

Where classification confidence is insufficient, the system must choose the safer handling path.

# 6. PROGRAMMED RESPONSES AND MODEL ROUTING

The existing `PerazimAiEngine.java` provides programmed responses through keyword rules.

This functionality should be retained and improved.

The system should select the cheapest reliable method for each question.

## 6.1 Deterministic answers

Use predefined responses when a question has a clear, approved answer.

Examples include:

- Church name.
- Mission and vision.
- Official contact details.
- Approved service information.
- App navigation instructions.
- Simple membership procedures.
- Frequently asked institutional questions.

These answers should not require Qwen generation when the approved answer can be returned directly.

## 6.2 Local knowledge retrieval

Where an answer depends on an approved document, retrieve the relevant passage and provide it to Qwen.

Examples include:

- What does the church constitution say about a particular matter?
- What is the approved procedure for a ministry?
- What does an approved discipleship resource teach?
- What is the church's official position on a theological question?

## 6.3 Local model generation

Use Qwen when interpretation, explanation, synthesis or conversational reasoning is required.

Examples include:

- Explaining a biblical passage.
- Comparing two theological interpretations.
- Explaining a historical Christian event.
- Structuring a Bible study from approved resources.
- Explaining a doctrine using relevant Scripture.

The model must not be used where a deterministic answer is more reliable.

# 7. LOCAL KNOWLEDGE RETRIEVAL

The existing Bible retrieval system must be extended into a general approved-knowledge retrieval system.

The local system must support retrieval from:

- Approved church documents.
- Official doctrine.
- Institutional information.
- Ministry resources.
- Approved Bible study materials.
- Licensed biblical content.
- Approved historical and theological resources.

## 7.1 Existing Bible retrieval

The current `BibleContextExtractor.java` uses local SQLite retrieval and keyword matching.

Codex must inspect its current implementation and improve it without breaking the Bible reader or existing Scripture features.

The current KJV default must be reviewed against the product's ESV-first preference.

The system must not package or redistribute ESV content without verified permission.

## 7.2 General document retrieval

A reusable retrieval component must be introduced or developed from existing structures.

Suggested responsibility:

`LocalKnowledgeRetriever.java`

Its responsibilities should include:

- Searching approved local knowledge.
- Filtering by audience and permissions.
- Filtering by approval status.
- Filtering withdrawn or expired material.
- Ranking relevant passages.
- Returning source references.
- Enforcing local package validity.
- Limiting retrieved content to the model's context budget.

The final class structure is subject to repository inspection.

## 7.3 Retrieval order

The initial implementation should prioritise:

1. Exact reference lookup.
2. Exact document lookup.
3. Structured metadata filtering.
4. Full-text search.
5. Keyword relevance ranking.
6. Optional semantic retrieval after evaluation.

Semantic retrieval must not override authority or permission filters.

A semantically similar document is not necessarily an authoritative answer.

## 7.4 Source preservation

Every retrieved passage must retain:

- Document identifier.
- Revision identifier.
- Source title.
- Section or heading.
- Page reference where available.
- Authority classification.
- Audience classification.

The generated answer should cite available source information.

# 8. SCRIPTURE INTEGRATION

Scripture must be integrated into the offline model's context.

This is a critical requirement because the existing local model does not currently receive the retrieved Scripture and church knowledge necessary to ground its responses.

When the question concerns a biblical passage, the system should retrieve the relevant passage and surrounding context.

It should avoid returning isolated verses where the surrounding passage materially changes interpretation.

For detailed biblical study, retrieval may include:

- The requested passage.
- Immediate literary context.
- Relevant cross-references.
- Relevant approved study notes.
- Licensed original-language resources where available.

The system must identify the translation accurately.

The ESV is the preferred translation, subject to verified licensing.

The system must not fabricate biblical quotations or references when retrieval fails.

# 9. PROMPT CONSTRUCTION

The local prompt must be centrally constructed.

The prompt must include:

1. Perazim AI identity.
2. Behaviour and communication instructions.
3. Theological authority rules.
4. Scope and safety instructions.
5. Relevant approved knowledge.
6. Relevant Scripture.
7. Source classifications.
8. Relevant conversational context.
9. Output instructions.

The prompt must be concise enough to fit the model's context window.

## 9.1 Source delimitation

Retrieved documents must be clearly separated from instructions.

For example, the system may use structured boundaries such as:

```text
SYSTEM RULES:
Follow the Perazim AI behaviour and theological authority policies.

APPROVED KNOWLEDGE:
Source: [document title]
Authority: [classification]
Revision: [revision]
Content:
[retrieved passage]

SCRIPTURE:
Translation: [translation]
Reference: [Bible reference]
Content:
[retrieved passage]

USER QUESTION:
[user question]
```

Retrieved document content must be treated as evidence, not as executable instructions.

This protects against malicious or accidental instructions embedded inside uploaded documents.

## 9.2 Context limits

The system must not send entire documents to Qwen unnecessarily.

It should retrieve only the most relevant passages and reserve context capacity for:

- System instructions.
- User question.
- Relevant conversation turns.
- Scripture.
- Approved church knowledge.
- Response generation.

Context allocation must be configurable and measured on actual supported devices.

# 10. CONVERSATION CONTEXT

Ordinary AI conversations must not be persistently stored as chat history during the pilot.

The existing ephemeral conversation context may be retained while the current AI dialog is active.

The system may use a limited number of recent messages to maintain conversational coherence.

When the dialog closes or the context is discarded, ordinary conversation history must not be retained as a permanent AI memory.

The assistant must not:

- Automatically remember personal confessions.
- Convert conversation content into church knowledge.
- Use private conversations to train models.
- Place ordinary conversation content into shared caches.

Minimal technical usage and security records may be retained under the approved retention policy, but must not contain unnecessary conversation text.

# 11. LOCAL PRIVACY AND SENSITIVE PASTORAL CONTENT

Sensitive pastoral conversations must not be sent to Gemini during the pilot.

The offline model should handle such content locally where technically appropriate.

Sensitive topics include:

- Marital difficulties.
- Family conflict.
- Abuse.
- Suicidal thoughts.
- Self-harm.
- Confidential pastoral confession.
- Private spiritual struggles.
- Other personally identifying or sensitive pastoral matters.

Detection must not depend solely on the user selecting a sensitivity option.

The system must use appropriate local detection and conservative routing.

If sensitivity is uncertain, the system should avoid sending the content online.

The offline model must still follow emergency safety instructions.

Where human follow-up is appropriate, the user may be offered the separate protected escalation process described in Document 09.

Offline escalation attempts must clearly show whether they have actually been submitted.

# 12. OFFLINE ACCESS AND PERMISSIONS

Offline access must respect the user's available knowledge package and permission state.

Public users may access approved public knowledge.

Verified members may access additional authorised resources where permitted.

Ministry workers and leadership may access restricted resources only where their permissions and the applicable package allow it.

The local application must not treat a user-selected role or locally editable preference as proof of authorisation.

## 12.1 Membership snapshots

The application may store a minimal, signed membership or permission snapshot for offline interface behaviour where necessary.

Such snapshots must:

- Be issued by the authoritative backend.
- Have a bounded validity period.
- Be protected against modification.
- Contain only the minimum necessary information.
- Be refreshed when connectivity returns.

The full membership register must never be packaged into the local AI knowledge base.

## 12.2 Revocation limitations

A disconnected device cannot immediately receive a membership revocation or permission change.

The system must acknowledge this limitation.

Restricted offline access must use bounded validity and revalidation rules.

Expired authorisation must not be silently treated as current.

# 13. OFFLINE KNOWLEDGE PACKAGE MANAGEMENT

The local AI must use approved and validated knowledge packages.

Package installation and updates must follow the rules in Document 04.

The application must:

- Verify signatures.
- Verify package integrity.
- Validate schema compatibility.
- Check audience restrictions.
- Check expiry.
- Activate updates atomically.
- Retain the previous valid package where appropriate.
- Record update status.

A failed package update must not unnecessarily disable the existing valid offline assistant.

The local assistant must disclose when the active package is outdated or incomplete.

# 14. MODEL PERFORMANCE AND RESOURCE MANAGEMENT

The current Qwen2.5 0.5B model is small.

It may be useful for lightweight conversational tasks, but its reasoning, theological accuracy, language comprehension and long-context performance must be evaluated rather than assumed.

Codex must measure:

- Model loading time.
- First-token latency.
- Generation speed.
- Peak memory usage.
- CPU utilisation.
- Battery consumption.
- Storage consumption.
- Stability during extended conversations.
- Behaviour under low-memory conditions.

Testing must use representative Android devices, not only desktop development environments.

## 14.1 Model failure handling

The system must handle:

- Missing model files.
- Corrupted model files.
- Unsupported device architecture.
- Insufficient memory.
- Inference runtime errors.
- User cancellation.
- Application backgrounding.
- Device sleep.
- Excessive generation time.

Failures must not crash the application.

Where possible, the system should offer deterministic approved answers or a clear limitation message.

It must not claim that the model successfully generated an answer when inference failed.

## 14.2 Model replacement

The architecture should allow future model upgrades without rewriting the entire AI system.

A future model may be considered if evaluation demonstrates materially better performance within acceptable device and licensing constraints.

No replacement should occur without documented comparison, device testing and regression testing.

# 15. RESPONSE VALIDATION

Generated answers must pass through a validation stage before being presented.

The system should check:

- Whether the question is within scope.
- Whether retrieved sources were authorised.
- Whether the answer makes unsupported institutional claims.
- Whether it fabricates Scripture references.
- Whether it claims access to current information.
- Whether it exposes restricted information.
- Whether it violates safety instructions.
- Whether it requires human escalation.

A small model cannot reliably validate every theological claim about itself.

Therefore, validation must combine deterministic controls, source-grounding rules and appropriate lightweight checks.

The assistant must not treat the model's own confidence statement as proof of correctness.

# 16. RESPONSE FORMATTING

Offline responses should be readable on a mobile screen.

Where appropriate, they should contain:

- Direct answer.
- Biblical references.
- Relevant explanation.
- Approved source references.
- Important qualifications.
- A clear limitation statement when necessary.

The system should avoid unnecessary verbosity for simple questions.

For deeper theological questions, it should provide adequate context and reasoning within the available model capability.

The assistant must never fabricate a citation to make an answer appear more authoritative.

# 17. OFFLINE LIMITATIONS

The assistant must clearly recognise what it cannot do offline.

It cannot reliably provide:

- Current internet research.
- Live news.
- Real-time service updates unless synchronised.
- Newly published church documents not yet downloaded.
- Immediate server-side membership changes.
- Immediate permission revocation.
- Live escalation submission without connectivity.
- Current information that is not contained in its local package.

When asked for such information, it must state the limitation and explain what can be done when connectivity returns.

It must not invent current information.

# 18. OFFLINE USAGE AND COST

Offline inference must not incur Gemini API charges.

The system must still monitor relevant local resource use, such as:

- Generation time.
- Device memory.
- Model failures.
- Package storage.
- Local processing load.

Local usage analytics should avoid collecting ordinary conversation content.

No persistent AI conversation history should be introduced during the pilot.

# 19. TESTING REQUIREMENTS

Offline AI must undergo testing on actual supported Android devices.

The evaluation must include:

## 19.1 Connectivity tests

- Airplane mode.
- No SIM or mobile data.
- No Wi-Fi.
- Intermittent connectivity.
- Reconnection after offline use.

## 19.2 Retrieval tests

- Exact Bible references.
- Questions about approved church documents.
- Questions about official doctrine.
- Questions involving multiple sources.
- Conflicting sources.
- Withdrawn documents.
- Expired packages.
- Restricted documents.
- Missing knowledge.

## 19.3 Theological tests

- Biblical interpretation.
- Doctrinal explanation.
- Historical theology.
- Apologetics.
- Alternative Christian interpretations.
- Personal theological opinions versus official church doctrine.
- Questions where Perazim has no established position.
- Attempts to manipulate the assistant into changing official doctrine.

## 19.4 Privacy and safety tests

- Sensitive pastoral questions.
- Suicide and self-harm language.
- Abuse disclosures.
- Private membership information.
- Attempts to retrieve leadership-only documents.
- Attempts to inject instructions through uploaded content.
- Attempts to force sensitive content into online mode.

## 19.5 Performance tests

- Low-memory devices.
- Long responses.
- Repeated generation.
- Model cancellation.
- App backgrounding.
- Device rotation.
- Model loading after application restart.
- Interrupted knowledge-package updates.

# 20. ACCEPTANCE CRITERIA

The offline AI is acceptable only when testing demonstrates that:

1. It operates without internet access.
2. The existing Qwen and llama.cpp integration is preserved unless a justified change is approved.
3. Approved church knowledge is retrieved locally.
4. Relevant Scripture is supplied to the local model.
5. ESV preference is implemented where licensing permits.
6. KJV is not silently presented as ESV.
7. Source references are retained.
8. Authority and audience classifications are enforced.
9. Restricted knowledge is not exposed to unauthorised users.
10. Sensitive pastoral information is not sent to Gemini.
11. Ordinary AI conversations are not persistently stored.
12. The local model follows the theological framework in Document 03.
13. Missing or outdated knowledge is disclosed.
14. Emergency safety behaviour is available offline.
15. Model failures do not crash the application.
16. Package updates are validated and safely activated.
17. Offline permissions have bounded validity.
18. The application remains stable on representative supported devices.
19. Existing Bible, media, community and other application functions remain unaffected.
20. The system does not claim to provide live internet information while offline.

---

**END OF DOCUMENT 05**
