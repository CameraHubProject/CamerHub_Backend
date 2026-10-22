-- Add external identities without changing existing passwords, roles or user IDs.
CREATE TABLE camerahub.auth_identities (
 provider varchar(20) NOT NULL CHECK (provider = 'google'),
 subject varchar(255) NOT NULL,
 user_id uuid NOT NULL REFERENCES camerahub.users(id) ON DELETE CASCADE,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(provider, subject),
 UNIQUE(provider, user_id)
);
