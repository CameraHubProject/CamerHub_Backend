CREATE TABLE camerahub.jwt_sessions (
 id uuid PRIMARY KEY,
 user_id uuid NOT NULL REFERENCES camerahub.users(id),
 refresh_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL,
 revoked boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX jwt_sessions_user ON camerahub.jwt_sessions(user_id);
