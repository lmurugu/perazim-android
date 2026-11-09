# 07 — RESPONSE ROUTING AND CACHING

## 1. Purpose

This document defines the response orchestration, routing, retrieval and caching requirements for Perazim Missions Church International AI.

The objective is to ensure that each user question is handled by the simplest reliable method capable of producing an accurate, safe and appropriately authorised answer.

Perazim AI must not send every question to a generative model. Many questions can be answered more reliably and economically through deterministic application logic, approved institutional responses or direct retrieval of verified information.

The system must also avoid treating a previous AI-generated response as an authoritative source merely because it was stored or retrieved.

This document applies to both offline and online AI modes.

## 2. Core Design Principles

The response orchestration system must follow these principles:

1. **Authority before convenience:** The system must establish whether the user is authorised to receive the requested information before retrieving or returning it.
2. **Scope before generation:** Questions outside the authorised Perazim AI scope must be filtered before they reach a generative model.
3. **Approved evidence before model knowledge:** Perazim-specific answers must be grounded in approved church knowledge, not assumed from the model's general training.
4. **Simplest suitable response method:** Use deterministic answers and direct retrieval where they are sufficient.
5. **Generative models as reasoning and language tools:** Qwen and Gemini may interpret questions and formulate responses, but they do not create institutional authority.
6. **Consistent behaviour across modes:** Offline and online AI must follow the same identity, theological, privacy and scope rules.
7. **No automatic knowledge creation:** A generated response must not become approved knowledge without the formal review and approval process.
8. **Cost and resource awareness:** Use retrieval and caching to reduce unnecessary token consumption, provider costs and device workload.
9. **Honest limitations:** The system must disclose when available knowledge, permissions or model capabilities are insufficient.
10. **Privacy by design:** Private pastoral information and restricted records must never leak through retrieval, cache or shared response paths.

## 3. Response Orchestration Architecture

### 3.1 Logical flow

Every request must pass through a shared orchestration policy, whether the selected execution mode is offline or online.

The logical flow is:

1. Receive and validate the request.
2. Identify the user and execution mode.
3. Evaluate access permissions.
4. Detect emergency and sensitive content.
5. Apply the Perazim scope filter.
6. Classify the question.
7. Identify the information and authority required.
8. Select an appropriate response method.
9. Retrieve authorised evidence where required.
10. Generate or assemble the response.
11. Validate the response.
12. Return the answer with appropriate source information and limitations.
13. Record permitted technical usage information without retaining ordinary conversation content.

The order is a logical description, not a requirement to execute every stage identically for every question. Emergency handling, privacy checks and access controls must take priority over ordinary caching and model generation.

### 3.2 Shared policy, separate execution engines

Offline Qwen and online Gemini must use the same authoritative behavioural policy.

The system may have separate execution adapters for local and remote generation, but the following rules must remain consistent:

- AI identity.
- Scope restrictions.
- Theological authority.
- Institutional knowledge authority.
- Sensitive information handling.
- Emergency handling.
- Privacy.
- Citation and source requirements.
- Response validation.

The system must not allow online Gemini to answer questions that offline Qwen would be prohibited from answering merely because Gemini is more capable.

### 3.3 Existing application components

Codex must inspect and reuse the existing implementation where appropriate.

Relevant existing components include:

- `PerazimAiEngine.java`
- `LocalPrompt.java`
- `OnDeviceAi.java`
- `AiRelayClient.java`
- `BibleContextExtractor.java`
- Existing programmed-response rules.
- Existing online AI request handling.
- Existing Room database and content retrieval components.
- Existing backend authentication and database services.

The response orchestration design must extend the existing application rather than introduce an unnecessary second AI architecture.

## 4. Request Classification

### 4.1 Purpose

Before selecting a response method, the system should classify the question according to its purpose and information requirements.

Classification is an aid to routing. It does not determine whether a question is true, approved or authorised.

The classifier may use deterministic rules, lightweight local processing or a model where necessary. A classification result must never override access controls or knowledge authority.

### 4.2 Recommended categories

| Category | Examples | Typical response method |
|---|---|---|
| Church identity | What is Perazim's mission? | Approved institutional answer |
| Church administration | What are the official contact details? | Approved record retrieval |
| Church doctrine | What does Perazim teach about baptism? | Approved doctrinal retrieval |
| Bible explanation | Explain Romans 8:28 | Scripture retrieval and suitable model |
| Bible reference | Where does Jesus teach about forgiveness? | Bible retrieval |
| Apologetics | How would Christianity respond to the problem of evil? | Knowledge retrieval and model reasoning |
| Discipleship | How can I grow in prayer? | Approved discipleship material and model |
| Pastoral care | How should I respond to grief as a Christian? | Safety assessment, approved guidance and suitable response |
| App assistance | How do I update my profile? | Programmed application instructions |
| Current research | What are the latest developments in a relevant theological debate? | Authorised online research, if available |
| Out-of-scope request | Give me investment advice unrelated to Christian ministry | Scope refusal and referral |

The categories may be extended as the application develops, but they must remain consistent with the project-wide scope policy.

### 4.3 Multi-category questions

Some questions require more than one category.

For example:

"According to Perazim's doctrine, how should I understand this passage?"

This requires both institutional doctrinal retrieval and biblical interpretation.

The system should identify the distinct information requirements and retrieve the relevant approved sources before generating an answer.

It must not allow a general theological answer to replace the official Perazim position when the user specifically asks what the church teaches.

### 4.4 Classification uncertainty

If classification confidence is low, the system should avoid making a high-impact assumption.

It may:

- Ask one focused clarification question.
- Use a conservative response method.
- Retrieve a broader but still permission-filtered set of sources.
- Refer the question for human review if it concerns an institutional or doctrinal decision.

The classifier must not invent a category or permission merely to proceed.

## 5. Response Method Selection

### 5.1 General priority

The system should select the least expensive and most reliable suitable response method.

The following is the recommended routing preference:

1. Emergency safety handling, where applicable.
2. Access, permission, privacy and scope enforcement.
3. Deterministic programmed response.
4. Exact approved-answer cache.
5. Direct retrieval of approved institutional or biblical information.
6. Local or server-side knowledge retrieval combined with Qwen or Gemini, according to the selected mode and permissions.
7. Honest limitation or human escalation when sufficient reliable evidence is unavailable.

This is not a rule that every question must pass through every method. It is a decision framework.

### 5.2 Deterministic programmed responses

Use deterministic responses for predictable application and institutional questions where the answer is fixed and approved.

Examples include:

- Basic navigation instructions.
- How to access an application feature.
- Stable church identity statements.
- Approved contact information.
- Fixed instructions for a known application workflow.

The existing programmed response engine should be preserved and improved where appropriate.

A deterministic response must have a known source or approved configuration. It must not become an uncontrolled collection of hardcoded institutional facts scattered throughout the application.

When an answer changes, the authoritative record or configuration must be updated through the appropriate process.

### 5.3 Exact approved-answer cache

An exact approved-answer cache may return a previously reviewed response when the current question matches an approved question or recognised equivalent.

It must be used only after access, permission, scope and privacy checks.

An exact cache entry must be associated with:

- A unique cache identifier.
- The approved question or recognised question variant.
- The approved answer.
- Source document identifiers.
- Source revisions.
- Approval status.
- Applicable audience and permissions.
- Language.
- Creation date.
- Review or expiry date.
- Knowledge revision or publication version.

The system must not return an answer after its source has been withdrawn, superseded or made inaccessible to the user.

### 5.4 Direct knowledge retrieval

When a user asks for information contained in an approved document, the system should retrieve the relevant passage rather than ask a generative model to reconstruct it from general knowledge.

Examples include:

- The wording of an approved church policy.
- A specific section of the Constitution.
- A ministry programme description.
- An approved doctrinal statement.
- A Bible passage.

Direct retrieval is particularly important where exact wording matters.

If the user requests an explanation rather than the wording itself, the retrieved evidence may be supplied to the appropriate model for explanation.

### 5.5 Generative response

Use Qwen or Gemini when the question requires interpretation, explanation, comparison, synthesis or conversational formulation beyond what a deterministic answer or direct quotation can adequately provide.

The model must receive relevant approved evidence where the question depends on Perazim-specific or biblical information.

The model must not be allowed to invent missing institutional evidence.

### 5.6 Human escalation

Where a question requires an authorised human decision, the system must not attempt to resolve it by choosing a more capable model.

Examples include:

- An unresolved conflict between official church documents.
- A request for a new institutional policy.
- A question requiring formal doctrinal determination.
- A confidential pastoral matter requiring human care.
- A membership or administrative dispute.
- A decision reserved for authorised church leadership.

The escalation procedure is defined in `09_PASTORAL_SAFETY_AND_ESCALATION.md` and the relevant administration requirements.

## 6. Offline and Online Routing

### 6.1 Offline mode

Offline requests must use resources available on the device.

The offline routing system may use:

- Deterministic programmed responses.
- Locally available exact approved-answer entries.
- Approved local church knowledge.
- Local Bible retrieval.
- Qwen local generation.

It must respect the audience and expiry restrictions of the active local knowledge package.

The system must not claim to have accessed current online research or information unavailable on the device.

### 6.2 Online mode

Online requests may use:

- Deterministic programmed responses.
- Authorised server-side exact approved-answer entries.
- Approved server-side knowledge retrieval.
- Gemini generation.
- Authorised external research where available.

Online access remains restricted to verified members under the online AI specification.

The system must not assume that an internet connection automatically grants permission to retrieve every server-side document.

### 6.3 Mode selection

The user may select offline or online AI where the application exposes that choice.

If the user selects online mode, the application must confirm that the account and request are eligible.

If the network is unavailable, the application may offer offline mode when the local engine is available.

Any automatic fallback must clearly identify the mode that produced the answer.

### 6.4 Mode consistency

A response produced offline and one produced online may differ in depth or wording because the models and available knowledge differ.

However, they must not contradict each other on an established approved Perazim position because of different behavioural instructions.

The system must use the same approved knowledge revision and authority rules wherever both modes have access to the relevant information.

## 7. Retrieval Architecture

### 7.1 Retrieval purpose

Retrieval supplies the model with relevant source material before it generates an answer.

It is not a substitute for access control, source approval or theological reasoning.

The retrieval system must identify the most relevant authorised evidence without exposing unrelated or restricted documents.

### 7.2 Retrieval sequence

The initial implementation should prioritise:

1. Exact document or reference lookup.
2. Structured metadata filtering.
3. Full-text search.
4. Relevance ranking.
5. Optional semantic retrieval in a later evaluated phase.

The implementation must reuse existing Bible retrieval and Room infrastructure where appropriate.

It should not introduce a complex vector database or embedding service before the simpler retrieval methods have been tested.

### 7.3 Metadata filtering

Before ranking content, the retrieval system must filter according to:

- Approval status.
- Authority level.
- Current revision.
- Effective dates.
- Withdrawal status.
- Audience.
- Assigned permissions.
- Content category.
- Offline eligibility.
- Licensing restrictions.

Filtering must happen before restricted material is returned to the model.

### 7.4 Relevance ranking

The system should rank retrieved material according to:

- Direct relevance to the question.
- Source authority.
- Exact reference match.
- Current revision.
- Contextual completeness.
- User permissions.

A high relevance score must not allow an unapproved source to outrank an authoritative approved source when answering a question about official Perazim doctrine or policy.

### 7.5 Retrieval limits

Retrieval must use a configurable token budget.

The system should retrieve only enough material to answer the question reliably.

It must avoid sending entire documents, entire membership lists, unrelated historical records or unnecessary personal information to the model.

### 7.6 Source traceability

Each retrieved passage must retain its source identity and revision.

The response system must be able to identify which source passages informed an answer.

Where user-facing citations are supported, they should identify the relevant document, section or biblical reference.

The system must not create a citation that does not correspond to an actual retrieved source.

### 7.7 Conflicting sources

If retrieved documents conflict, the system must apply the relevant authority hierarchy and document-status rules.

A newer document must not automatically override a constitutional provision or formal doctrinal statement without authorised amendment or clarification.

If the conflict cannot be resolved from the documented authority hierarchy, the system must disclose the conflict and refer it for authorised review.

## 8. Caching Architecture

### 8.1 Purpose

Caching reduces repeated computation, provider costs and response times.

It must not weaken the authority, privacy or access requirements of the knowledge system.

A cache is a performance mechanism, not an independent source of truth.

### 8.2 Cache categories

The system must distinguish at least three categories.

**Category A — Approved institutional answers**

These are reviewed and approved answers associated with authoritative church sources.

They may be reused when the question, language, audience, permissions and source revisions match.

**Category B — Verified general Christian information**

These are responses based on reliable biblical, historical or theological sources but are not official Perazim doctrinal statements.

They must be clearly distinguished from institutional answers.

**Category C — Unverified generated responses**

These are ordinary generated answers that have not undergone formal review and approval.

They must not automatically become reusable authoritative answers.

During the initial implementation, the system should not create a shared cache of arbitrary generated answers.

### 8.3 Exact cache first

The first caching implementation should use exact matching or explicitly approved question variants.

An exact cache entry must be checked against the current source revision and user permissions before it is returned.

The system must not use similarity alone to conclude that two questions have identical meaning.

For example:

- "What is the church's official position on baptism?"
- "What does the Bible teach about baptism?"

These are related questions but do not necessarily require the same answer.

### 8.4 Semantic caching

Semantic caching may be considered after the exact cache and retrieval system have been evaluated.

Semantic similarity means that two questions have similar language or meaning. It does not prove that they require the same answer.

If introduced, semantic matching should initially be used to retrieve candidate knowledge or previously reviewed answers for further validation.

It must not automatically return a previous generated response solely because a similarity threshold has been reached.

Before returning any semantically matched answer, the system must verify:

- Intent equivalence.
- Relevant context.
- Source authority.
- Source currency.
- Audience and permissions.
- Language.
- Whether the answer actually addresses the user's question.

### 8.5 Cache key design

Cache keys must account for the factors that can change the validity of an answer.

Depending on the cache category, these include:

- Normalised question or approved question identifier.
- Language.
- Question category.
- AI mode where relevant.
- Knowledge revision.
- Source revision.
- Audience and permission scope.
- Response-policy version.
- Approval status.

The implementation must avoid cache keys that allow a response generated for one permission level to be returned to another.

### 8.6 Cache invalidation

A cache entry must become invalid when:

- Its source is withdrawn.
- Its source is replaced.
- Its source revision changes.
- Its approval is revoked.
- Its effective date expires.
- Its audience restrictions change.
- The user's permission no longer allows access.
- Its review or expiry date is reached.
- The governing response policy changes in a way that affects the answer.

Invalidation must be reliable across server-side and local caches.

### 8.7 Cache storage

The server may maintain approved answer caches in PostgreSQL or another appropriately controlled storage mechanism.

The Android application may maintain a local cache of approved answers where offline use is permitted.

Local cache entries must carry the source and revision metadata required for validation.

Restricted entries must not be included in public offline packages.

### 8.8 No private pastoral caching

Private pastoral disclosures, identifiable member information and individual escalation records must never enter shared response caches.

An individual user's private information must not become a reusable response for another user.

## 9. Cache and Knowledge Lifecycle

### 9.1 Knowledge remains authoritative

The knowledge repository is the authoritative source for approved church content.

A cached response is valid only while its underlying approved source and permissions remain valid.

### 9.2 Generated response promotion

A generated response may be proposed for review if it identifies a useful knowledge gap or a frequently asked question.

The promotion process must be:

1. Identify the proposed answer or knowledge gap.
2. Identify supporting authoritative sources.
3. Verify the content.
4. Assign the correct knowledge category and authority.
5. Obtain the required approval.
6. Publish through the knowledge-management process.
7. Generate or update the appropriate approved cache entry.

A model must not approve its own response.

### 9.3 Cache as a knowledge-gap signal

The system may analyse aggregate question categories and cache misses to identify:

- Frequently asked questions.
- Missing institutional information.
- Unclear teaching materials.
- Areas where users repeatedly need human assistance.
- Topics requiring improved discipleship resources.

This analysis must not require permanent retention of ordinary conversation transcripts.

Reports should use minimal aggregate information and avoid exposing private member content.

## 10. Token and Resource Management

### 10.1 Token budgets

The system must use token-based limits for prompts, retrieved context and model output.

Character counts alone are insufficient because different languages and content types consume different token quantities.

### 10.2 Context prioritisation

When the available context budget is limited, prioritise:

1. Mandatory safety and behavioural instructions.
2. Applicable Perazim doctrinal and institutional authority.
3. Directly relevant approved evidence.
4. Relevant biblical text and context.
5. Necessary recent conversational context.
6. Optional explanatory background.

The system must not remove essential safety or authority instructions merely to fit more retrieved content.

### 10.3 Model-specific limits

Offline Qwen and online Gemini may have different practical context and output limits.

The orchestrator must apply the correct budget for the selected model.

The implementation must not assume that the two models have identical capabilities.

### 10.4 Cost-aware routing

Where two methods can answer a question with equivalent reliability, the system should prefer the lower-cost method.

For example, an approved contact number should be retrieved directly rather than sent to Gemini.

However, cost optimisation must never justify returning an outdated, unapproved or unauthorised answer.

## 11. Response Validation

### 11.1 Validation purpose

A generated answer must be checked against the request, available evidence and applicable rules.

The system should use deterministic validation wherever possible, supported by additional model-based evaluation only where appropriate.

### 11.2 Validation checks

The response should be checked for:

- Scope compliance.
- Permission compliance.
- Consistency with approved Perazim positions.
- Evidence support.
- Correct biblical references.
- Correct Bible translation labels.
- Source citation validity.
- Privacy leakage.
- Unsupported institutional claims.
- Unsafe pastoral guidance.
- Claims of live research that did not occur.

### 11.3 Failure behaviour

If a response fails validation, the system must not return it as though it were reliable.

Depending on the failure, the system may:

- Retry with corrected context.
- Use direct approved retrieval.
- Return an honest limitation.
- Ask for clarification.
- Escalate to an authorised human.

Retries must remain subject to usage and cost controls.

## 12. Implementation Strategy

The implementation should proceed incrementally.

### Phase 1 — Deterministic routing and exact retrieval

- Inspect the existing programmed response engine.
- Identify duplicate or conflicting response rules.
- Preserve valid application-help responses.
- Centralise applicable routing decisions.
- Extend existing Bible retrieval where necessary.
- Add approved church knowledge retrieval.
- Enforce approval and permission filters.
- Add source references.

### Phase 2 — Exact approved-answer caching

- Define the cache schema.
- Introduce approved cache entries.
- Implement exact matching and approved variants.
- Add revision-aware invalidation.
- Enforce audience and permission restrictions.
- Test cache isolation.

### Phase 3 — Shared model orchestration

- Integrate Qwen and Gemini through the same policy layer.
- Provide relevant approved evidence to both.
- Apply model-specific token budgets.
- Implement consistent response validation.
- Test offline and online consistency.

### Phase 4 — Evaluation and optimisation

- Collect privacy-safe aggregate performance metrics.
- Evaluate routing accuracy.
- Evaluate retrieval relevance.
- Measure cache hit rates.
- Measure response quality and cost.
- Identify repeated knowledge gaps.
- Consider semantic retrieval only when justified by evaluation results.

## 13. Testing and Acceptance Criteria

### 13.1 Routing

The implementation must demonstrate that:

- Simple application questions use deterministic responses where appropriate.
- Approved institutional questions retrieve authoritative sources.
- Bible questions use the appropriate Scripture retrieval process.
- Complex explanatory questions use a suitable model.
- Out-of-scope questions are rejected or redirected.
- Human-authority questions are escalated rather than decided by AI.
- Emergency handling takes priority over ordinary routing.

### 13.2 Authority

The implementation must demonstrate that:

- Official Perazim doctrine is not replaced by generic model opinion.
- Unapproved documents cannot be treated as approved doctrine.
- Withdrawn documents cannot remain authoritative through cache.
- External research remains distinct from institutional knowledge.
- Conflicting authoritative sources are disclosed or escalated appropriately.

### 13.3 Offline and online consistency

The implementation must demonstrate that:

- Both modes follow the same scope and doctrinal rules.
- Both modes respect their applicable knowledge permissions.
- Offline mode does not claim live research.
- Online mode cannot bypass restrictions imposed on offline mode.
- Switching modes does not expose another user's context.

### 13.4 Cache integrity

The implementation must demonstrate that:

- Only eligible cache entries are returned.
- Cache entries are validated against source revisions.
- A withdrawn source invalidates dependent cache entries.
- Permission changes prevent unauthorised cache retrieval.
- Similar but materially different questions do not receive inappropriate identical answers.
- Private pastoral information is never shared through a cache.
- Generated answers are not automatically promoted into approved knowledge.

### 13.5 Performance and cost

The implementation must measure:

- Deterministic response rate.
- Exact cache hit rate.
- Retrieval latency.
- Model invocation rate.
- Average input and output tokens.
- Provider cost per request.
- Failure and retry rates.
- Offline generation performance.

The system must demonstrate that routing and caching reduce unnecessary model use without reducing answer reliability.

## 14. Explicitly Out of Scope for Initial Implementation

The following are deferred unless separately approved:

- Automatic promotion of generated answers.
- Unreviewed shared semantic response caching.
- Model fine-tuning on member conversations.
- Automatic doctrine discovery.
- Complex vector database infrastructure before retrieval evaluation.
- Persistent storage of ordinary conversation transcripts.
- Cross-user personalised answer sharing.
- Automatic changes to approved knowledge based on model confidence.

## 15. Implementation Authority

Codex must inspect the current routing, programmed-response, Bible retrieval, local model and online model implementations before proposing code changes.

The objective is to improve the existing architecture while preserving application functionality.

Codex must identify any conflict between this specification and existing behaviour.

This document is part of the complete project specification. It does not independently authorise implementation. Code changes may begin only after the complete documentation set has been reviewed and the project owner has supplied the final master implementation instruction.
