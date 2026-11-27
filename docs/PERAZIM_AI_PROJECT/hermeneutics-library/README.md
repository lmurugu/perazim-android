# Perazim Offline Hermeneutics Library

This directory is the agent-facing acquisition workspace for the Perazim offline biblical research library.

## Owner-selected target

- Distribution: Perazim church ministry.
- Storage: individual Android devices.
- Runtime: fully offline after local installation.
- Scope: largest academically credible and lawfully distributable collection.
- Existing architecture: Java Android + Room/SQLite + existing Qwen2.5 0.5B GGUF / llama.cpp runtime.

## Start here

1. Read [Document 17 — Offline Biblical Hermeneutics Library Specification](../17_HERMENEUTICS_LIBRARY_SPECIFICATION.md).
2. Read [Hermeneutical Protocol](HERMENEUTICAL_PROTOCOL.md) before changing Bible-answer behaviour.
3. Read [Resource Catalogue](RESOURCE_CATALOG.md) for the human-readable list.
4. Parse [RESOURCE_CATALOG.json](RESOURCE_CATALOG.json) for machine-readable resource metadata and download routes.
5. Follow [Acquisition and Validation Runbook](ACQUISITION_AND_VALIDATION_RUNBOOK.md) before downloading or converting any dataset.

## Critical status warning

**This directory contains research specifications and candidate source metadata. It does not contain the third-party corpora themselves.** A listed resource is not automatically approved for download, redistribution, AI processing or device installation. The current licence, file-level notices, third-party components, version, source integrity and technical quality must be checked before admission.

Do not add large upstream corpora, Bible text archives, PDFs, model files or other third-party datasets to this Git repository. The repository stores manifests, scripts, attribution records and specifications. Lawfully acquired content is prepared as separately versioned local resource packs.

## Authority and use

Scripture is the supreme authority for Christian doctrine. Original-language resources provide linguistic evidence; ancient texts provide historical evidence; commentaries and Church Fathers provide attributed interpretations. No external resource becomes official Perazim doctrine merely by being included in this library.

## Licence warning

- Crossway currently prohibits AI processing of ESV text for interpretation or AI-generated answers in its published terms. ESV text is excluded from the AI corpus unless written permission expressly authorises the proposed use.
- Sefaria, ORACC, Perseus, CDLI and other aggregations have item-, project- or component-specific rights. Do not bulk-import an entire export based on a repository-level label.
- CC BY-NC and CC BY-SA resources require separate review. A church-only application is not automatic proof that a use qualifies as non-commercial.
- The current repository Bible rights record is an existing claim, not a substitute for rechecking each text, edition and jurisdiction.

## Status vocabulary

- CANDIDATE: identified and listed; no admission decision.
- SOURCE_ROUTE_CHECKED: an official repository or download route was found.
- LICENCE_CHECKED: applicable licence evidence was checked for the exact content/component.
- QUALITY_CHECKED: data integrity and sample quality have been checked.
- OWNER_REVIEW_REQUIRED: a reserved rights, doctrine, translation or deployment decision is needed.
- APPROVED_FOR_PACKAGING: all required gates passed and approval is recorded.
- REJECTED: not suitable or rights do not permit intended use.

Update the JSON catalogue when a candidate changes status. Never infer a higher status from the presence of a URL.