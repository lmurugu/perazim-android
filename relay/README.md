# Perazim AI relay Worker

This Cloudflare Worker source is the provider relay for the online AI path. It is not called directly by Android. The intended request path is:

```text
Android → authenticated Express API → signed short-lived grant → Worker → Gemini
```

The checked-in Android client and online feature gates currently keep this path unavailable to members. Source code or a passing mock test does not show that a Worker is deployed or operational. This document describes the repository contract only; it does not authorize deployment or provider activation.

## Request contract

Express sends a `POST` request to `/chat` with a bearer JWT grant and a JSON body containing exactly:

```json
{
  "requestId": "<grant jti UUID>",
  "question": "<1–500 character question>"
}
```

The Worker validates the signature, issuer, audience, subject pseudonym, issue/expiry window, request ID, enablement flag, and question shape. The request ID must match the grant's `jti`. Android tokens, client identity, permissions, membership claims, and conversation history are not accepted. The Worker also requires its KV binding and applies a defensive daily count; authoritative atomic quota enforcement remains an Express prerequisite before activation.

## Provider instruction boundary

The Gemini request uses the provider's `systemInstruction` field for the fixed application-controlled instruction text and one `contents` item with role `user` for the untrusted question. User text must never be interpolated into the system instruction. A separate field establishes the prompt boundary; it does not by itself prevent every prompt-injection outcome or replace approved-source retrieval and response validation.

The Worker does not log question text or bearer grants. Provider errors return controlled responses; logs contain limited upstream status/timeout information. The configured provider credential is supplied through the Worker environment and must never be committed or printed.

## Environment bindings

Runtime bindings used by the source:

- `AI_ENABLED` — explicit Worker kill switch; must remain disabled until all activation prerequisites are approved and verified.
- `PERAZIM_AI_WORKER_SHARED_SECRET` — shared signing secret used to verify Express grants.
- `GEMINI_API_KEY` — provider credential, supplied as a protected runtime secret.
- `PERAZIM_KV` — defensive quota storage binding.

The checked-in `wrangler.toml` names the Worker and KV binding. Deployment state and secret configuration have not been verified by this repository audit.

## Local contract tests

From the repository root:

```sh
node relay/test/conversation.test.mjs
node server/test/worker_service_auth.test.js
node server/test/online_ai_contract.test.js
```

These tests use synthetic signed grants and mocked provider responses. They do not contact Gemini, inspect a live database, or establish deployed Worker behavior.
