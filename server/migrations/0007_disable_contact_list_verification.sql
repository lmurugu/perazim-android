-- DATA-01: demote legacy CSV-derived verification; a human administrator must confirm membership.
WITH demoted AS (
    UPDATE membership_verifications
       SET status='REGISTERED', verification_source='MANUAL', revision=revision+1, updated_at=(extract(epoch FROM clock_timestamp()) * 1000)::bigint
     WHERE verification_source='CONTACT_LIST' AND status='VERIFIED_MEMBER'
     RETURNING user_id
)
INSERT INTO membership_verification_history (id,user_id,claim_id,previous_status,new_status,actor_id,reason,created_at)
SELECT md5('DATA-01-auto-contact-demotion:' || user_id)::uuid,
       user_id,NULL,'VERIFIED_MEMBER','REGISTERED',NULL,
       'DATA-01 legacy automatic contact-list verification disabled',
       (extract(epoch FROM clock_timestamp()) * 1000)::bigint
  FROM demoted
ON CONFLICT (id) DO NOTHING;

UPDATE membership_verifications SET verification_source='MANUAL'
 WHERE verification_source='CONTACT_LIST';

ALTER TABLE membership_verifications DROP CONSTRAINT IF EXISTS membership_verifications_source_check;
ALTER TABLE membership_verifications ADD CONSTRAINT membership_verifications_source_check
    CHECK (verification_source='MANUAL');
ALTER TABLE membership_verifications DROP CONSTRAINT IF EXISTS membership_verifications_verified_link_check;
ALTER TABLE membership_verifications ADD CONSTRAINT membership_verifications_verified_link_check
    CHECK (status <> 'VERIFIED_MEMBER' OR membership_record_id IS NOT NULL);
