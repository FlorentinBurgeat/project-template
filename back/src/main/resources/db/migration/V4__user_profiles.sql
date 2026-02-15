-- V4: User profiles table (foundation for future business data)
-- Identity data lives in Keycloak; this table stores app-specific profile data.

CREATE TABLE IF NOT EXISTS user_profiles (
    id              BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(100),
    bio             TEXT,
    avatar_url      VARCHAR(500),
    preferences     JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_user_profiles_keycloak_user_id ON user_profiles (keycloak_user_id);
