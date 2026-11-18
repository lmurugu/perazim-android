# ADR 0003: Rate limiting uses memory stores at launch

Status: Accepted (owner approval of the review corrections, 2026-11-18)

## Context
Rate limits protect sign-in, sign-up, PIN changes, privacy requests and the whole API from guessing and flooding. The limits are kept in process memory, so they reset when the server restarts and are not shared between server instances. Plan 3.5 asked for a PostgreSQL-backed store.

## Decision
- Launch with memory stores. The church runs one API instance, so a shared store is not needed yet.
- Limits in place:
  - whole API: 1000 requests per minute per address;
  - sign-up: 10 per address per hour, and 5 per phone number per hour;
  - PIN change: 10 per account per 15 minutes;
  - privacy requests: 10 per account per hour;
  - sign-in and membership claims: their existing custom limiters.
- Proxy address handling follows the `TRUST_PROXY_HOPS` setting, so forwarded headers cannot be used to dodge limits unless a trusted proxy sets them.

## Consequences
- A restart clears the counters. An attacker who can force restarts gets more attempts. The server should be restarted only for deployments, and the restart is logged.
- Moving to a shared store is required before a second instance is run, and is a follow-up task. The store is one `store` option per limiter, so the change is local.
