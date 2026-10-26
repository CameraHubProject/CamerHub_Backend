CREATE SCHEMA IF NOT EXISTS camerahub;
SET search_path TO camerahub,public;

CREATE TABLE advisor_conversations(id uuid PRIMARY KEY,owner_key varchar(64) NOT NULL,messages jsonb NOT NULL DEFAULT '[]'::jsonb,lease_token uuid,leased_until timestamptz,updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE advisor_usage(day date NOT NULL,client_key varchar(64) NOT NULL,requests integer NOT NULL DEFAULT 0,PRIMARY KEY(day,client_key));


CREATE TABLE integration_outbox(event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));
CREATE INDEX integration_outbox_pending ON integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE TABLE integration_inbox(event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE advisor_rate_windows(client_key varchar(64) NOT NULL,minute bigint NOT NULL,requests integer NOT NULL DEFAULT 0,PRIMARY KEY(client_key,minute));
