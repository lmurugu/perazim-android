-- Community review permission. An admin can grant it to other accounts through the existing
-- permission grant route. The church admin role keeps its access without a grant.
-- Draft for owner review. Not deployed. Numbered after 0025 (open groups and join requests).

INSERT INTO permissions (code, description, active) VALUES
    ('community.review', 'Approve or reject community posts and event photos', TRUE)
ON CONFLICT (code) DO NOTHING;
