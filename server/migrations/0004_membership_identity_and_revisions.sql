-- Profile display names are editable. Capture the registration name once;
-- legacy display names cannot establish a trustworthy registration snapshot.
CREATE TABLE membership_account_identities (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    registered_name TEXT NOT NULL
);

CREATE FUNCTION capture_membership_registration_identity() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO public.membership_account_identities (user_id, registered_name)
    VALUES (NEW.id, NEW.name);
    RETURN NEW;
END;
$$;

CREATE TRIGGER capture_membership_registration_identity
    AFTER INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION capture_membership_registration_identity();

-- Retain replaced register identity fields separately from general audit events.
CREATE TABLE membership_record_revisions (
    record_id UUID NOT NULL REFERENCES membership_records(id),
    revision INT NOT NULL CHECK (revision > 0),
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (record_id, revision)
);

CREATE FUNCTION reject_membership_record_revision_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_record_revisions is append-only';
END;
$$;

CREATE TRIGGER membership_record_revisions_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_record_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_membership_record_revision_mutation();
CREATE TRIGGER membership_record_revisions_no_truncate
    BEFORE TRUNCATE ON membership_record_revisions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_record_revision_mutation();
