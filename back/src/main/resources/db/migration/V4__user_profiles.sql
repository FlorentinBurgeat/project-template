-- V4: No-op.
--
-- This migration used to create a 'user_profiles' table that no code used. Identity data
-- lives in Keycloak, and app-specific profile data belongs to a profile bounded context,
-- which owns its own schema and migrations (ADR 0005), so the template ships no table.
--
-- Kept as a no-op to preserve the version sequence (V3 -> V4 -> next).
SELECT 1;
