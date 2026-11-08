-- Allow a phone matched directly against the maintained contact CSV to be
-- verified without fabricating a separately imported membership record.
ALTER TABLE membership_verifications
    ADD COLUMN verification_source TEXT NOT NULL DEFAULT 'MANUAL';

ALTER TABLE membership_verifications
    ADD CONSTRAINT membership_verifications_source_check
    CHECK (verification_source IN ('MANUAL', 'CONTACT_LIST'));

ALTER TABLE membership_verifications
    DROP CONSTRAINT membership_verifications_check;

ALTER TABLE membership_verifications
    ADD CONSTRAINT membership_verifications_verified_link_check
    CHECK (status <> 'VERIFIED_MEMBER' OR membership_record_id IS NOT NULL OR verification_source = 'CONTACT_LIST');
