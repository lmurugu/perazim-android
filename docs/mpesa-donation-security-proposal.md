# M-Pesa donation confirmation proposal

**Status:** Proposal only. No donation or payment code is included in this change.

## Server-owned payment state

The server creates a donation intent and its initial `PENDING` record. The app may display that state, but it cannot set or submit `CONFIRMED`. Daraja consumer credentials and the passkey remain server-side.

## Confirmation flow

1. The server creates the Daraja STK request and stores its checkout request identifiers with the pending donation intent.
2. Daraja sends its callback to the server. The server validates the callback against the stored request and records it as a candidate result.
3. The server calls Daraja's transaction-status endpoint and compares the returned result with the callback and stored intent: transaction receipt, checkout/request identifiers, shortcode, amount, and outcome must agree.
4. Only after that check succeeds does a server-side transaction change the record to `CONFIRMED`. A callback alone cannot confirm a donation.
5. If a callback is missing or status is temporarily unavailable, the record remains `PENDING` while the server retries status confirmation. A terminal failure can become `FAILED` or `EXPIRED`; it never contributes to totals.

## Replay and uniqueness rules

- Store Daraja's transaction receipt/transaction ID under a database unique constraint.
- A repeated callback for an already processed request or receipt is rejected from applying again. It may receive an acknowledgement that identifies it as a duplicate so Daraja does not needlessly retry delivery.
- A receipt already assigned to another intent is rejected and flagged for review; it cannot confirm either record a second time.
- Confirmation, receipt reservation, and audit entry commit atomically.

## Totals and local display

- Server totals and donation history include only rows whose server-owned state is `CONFIRMED`.
- Pending, failed, expired, or locally created records never contribute to totals.
- Any local-only payment record is visibly labelled **“Unverified”** and is not presented as a completed donation.
- The app refreshes confirmed history from the server; cached or client-supplied state cannot promote a record.

## Operational controls

Keep callback credentials and Daraja credentials out of the APK. Restrict the callback endpoint, retain an audit trail for status checks and state changes, and provide a reconciliation process for pending requests whose callback was lost. Before implementation, confirm Daraja's current callback authentication and transaction-status fields for the intended account and environment.

**Approval gate:** This is a design proposal for review. Do not implement payment behavior until the project owner approves it.
