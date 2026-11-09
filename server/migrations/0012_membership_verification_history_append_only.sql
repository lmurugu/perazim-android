-- Protect membership verification history from ordinary row and table mutation.
-- Database owners and superusers can still alter or disable these protections.

CREATE FUNCTION reject_membership_verification_history_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_verification_history is append-only';
END;
$$;

CREATE TRIGGER membership_verification_history_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_verification_history
    FOR EACH ROW EXECUTE FUNCTION reject_membership_verification_history_mutation();

CREATE TRIGGER membership_verification_history_no_truncate
    BEFORE TRUNCATE ON membership_verification_history
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_verification_history_mutation();
