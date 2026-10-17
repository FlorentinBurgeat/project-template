-- V3: Keycloak Integration Migration
--
-- User authentication is managed by Keycloak (separate database: keycloak_db).
-- The legacy 'users' and 'refresh_tokens' tables (V1/V2) were removed from the
-- template, so there is nothing left to migrate here.
--
-- This file is kept as a no-op to preserve the version sequence (V3 -> V4).
SELECT 1;
