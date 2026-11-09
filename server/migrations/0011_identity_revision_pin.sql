-- DATA-01: pin identity reconciliation approval to the exact reviewed record revision.
ALTER TABLE membership_identity_reconciliation_requests
    ADD COLUMN membership_record_revision INTEGER CHECK (membership_record_revision IS NULL OR membership_record_revision > 0);

ALTER TABLE membership_identity_reconciliation_events
    ADD COLUMN membership_record_revision INTEGER CHECK (membership_record_revision IS NULL OR membership_record_revision > 0);

-- The migration adds nullable audit metadata only; there is no backfill from mutable profile data.
