-- V3: Keycloak Integration Migration
--
-- This migration marks the transition from custom JWT authentication to Keycloak.
--
-- IMPORTANT NOTES:
-- 1. The 'users' and 'refresh_tokens' tables are now DEPRECATED
-- 2. User authentication is now managed by Keycloak
-- 3. Keycloak stores its own data in a separate database (keycloak_db)
--
-- MIGRATION STRATEGY:
-- For existing projects (not templates), you should:
-- 1. Export existing users from 'users' table
-- 2. Import them into Keycloak using the Admin API
-- 3. Map user IDs to Keycloak user IDs in a migration table
-- 4. After migration is complete, drop the old tables
--
-- For new projects (fresh template forks):
-- 1. These tables can be safely ignored or dropped
-- 2. All user management goes through Keycloak
--
-- This migration adds comments to mark tables as deprecated
-- The tables are NOT dropped to preserve data for reference/migration

-- Add comments to deprecated tables
COMMENT ON TABLE users IS 'DEPRECATED: User authentication moved to Keycloak. This table is kept for reference only.';
COMMENT ON TABLE refresh_tokens IS 'DEPRECATED: Refresh tokens now managed by Keycloak. This table is kept for reference only.';

-- Optional: Create a user migration tracking table if you need to migrate existing users
CREATE TABLE IF NOT EXISTS keycloak_user_migration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_user_id UUID NOT NULL REFERENCES users(id),
    keycloak_user_id VARCHAR(255) NOT NULL,
    migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(legacy_user_id),
    UNIQUE(keycloak_user_id)
);

COMMENT ON TABLE keycloak_user_migration IS 'Maps legacy user IDs to Keycloak user IDs for data migration purposes';

-- Optional: You can drop the deprecated tables after migration is complete
-- Uncomment these lines when you are ready to remove the old tables:
-- DROP TABLE IF EXISTS refresh_tokens CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;
-- DROP TABLE IF EXISTS keycloak_user_migration CASCADE;
