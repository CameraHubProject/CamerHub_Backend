CREATE SCHEMA IF NOT EXISTS camerahub;
SET search_path TO camerahub,public;
CREATE TABLE users (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), email text NOT NULL, password_hash text NOT NULL,
 full_name varchar(160) NOT NULL, phone varchar(20), account_status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK (account_status IN ('ACTIVE','SUSPENDED','CLOSED')),
 email_verified_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE roles (code varchar(20) PRIMARY KEY CHECK (code IN ('BUYER','SELLER','INSPECTOR','ADMIN')));
CREATE TABLE user_roles (
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 role_code varchar(20) NOT NULL REFERENCES roles(code), PRIMARY KEY(user_id,role_code)
);
CREATE TABLE user_addresses (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES users(id),
 recipient_name varchar(160) NOT NULL, phone varchar(20) NOT NULL, province text NOT NULL, district text,
 ward text, street_address text NOT NULL, is_default boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE audit_logs (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), actor_id uuid REFERENCES users(id),
 entity_type varchar(80) NOT NULL, entity_id uuid NOT NULL, action varchar(80) NOT NULL,
 old_data jsonb, new_data jsonb, correlation_id text, occurred_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO roles(code) VALUES ('BUYER'),('SELLER'),('INSPECTOR'),('ADMIN');
CREATE UNIQUE INDEX uq_users_email_lower ON users(lower(email));
CREATE UNIQUE INDEX uq_default_address ON user_addresses(user_id) WHERE is_default;
ALTER TABLE users ADD COLUMN profile_version bigint NOT NULL DEFAULT 0;
-- Add external identities without changing existing passwords, roles or user IDs.
CREATE TABLE camerahub.auth_identities (
 provider varchar(20) NOT NULL CHECK (provider = 'google'),
 subject varchar(255) NOT NULL,
 user_id uuid NOT NULL REFERENCES camerahub.users(id) ON DELETE CASCADE,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(provider, subject),
 UNIQUE(provider, user_id)
);
-- Preserve existing phone values; canonicalize only the unique sign-in key.
CREATE FUNCTION camerahub.normalized_login_phone(value text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
 SELECT CASE
   WHEN number ~ '^0[35789][0-9]{8}$' THEN '+84' || substring(number FROM 2)
   WHEN number ~ '^84[35789][0-9]{8}$' THEN '+' || number
   WHEN number ~ '^\+84[35789][0-9]{8}$' THEN number
   ELSE NULL
 END FROM (SELECT regexp_replace(value, '[[:space:]().-]', '', 'g') AS number) cleaned
$$;

-- Also prevents duplicates inserted concurrently or via another writer.
-- Existing duplicates must be resolved by their owners; never discard/merge accounts.
CREATE UNIQUE INDEX uq_users_login_phone
 ON camerahub.users(camerahub.normalized_login_phone(phone))
 WHERE camerahub.normalized_login_phone(phone) IS NOT NULL;

CREATE TABLE spring_session(primary_id char(36) PRIMARY KEY,session_id char(36) NOT NULL UNIQUE,creation_time bigint NOT NULL,last_access_time bigint NOT NULL,max_inactive_interval integer NOT NULL,expiry_time bigint NOT NULL,principal_name varchar(100));
CREATE INDEX spring_session_expiry ON spring_session(expiry_time);
CREATE INDEX spring_session_principal ON spring_session(principal_name);
CREATE TABLE spring_session_attributes(session_primary_id char(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,attribute_name varchar(200) NOT NULL,attribute_bytes bytea NOT NULL,PRIMARY KEY(session_primary_id,attribute_name));


CREATE TABLE integration_outbox(event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));
CREATE INDEX integration_outbox_pending ON integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE TABLE integration_inbox(event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());
