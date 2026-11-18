-- Monotonic identifiers for signed offline package control snapshots and rollback authorizations.
-- Production application requires the separately approved migration plan.
CREATE SEQUENCE ai_offline_control_sequence AS BIGINT MINVALUE 1 START WITH 1 NO CYCLE;
