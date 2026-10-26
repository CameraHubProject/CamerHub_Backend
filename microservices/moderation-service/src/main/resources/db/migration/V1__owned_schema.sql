CREATE SCHEMA IF NOT EXISTS camerahub;
SET search_path TO camerahub,public;

CREATE TABLE review_jobs(listing_id uuid NOT NULL,revision bigint NOT NULL,owner_id uuid NOT NULL,snapshot jsonb NOT NULL,state varchar(24) NOT NULL DEFAULT 'QUEUED',note text,source varchar(12),claim_token uuid,leased_until timestamptz,attempts integer NOT NULL DEFAULT 0,decision_id uuid,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),PRIMARY KEY(listing_id,revision));
CREATE TABLE listing_index(listing_id uuid PRIMARY KEY,revision bigint NOT NULL,snapshot jsonb NOT NULL,listing_status varchar(24) NOT NULL,updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE review_decisions(id uuid PRIMARY KEY,listing_id uuid NOT NULL,revision bigint NOT NULL,status varchar(24) NOT NULL,note text NOT NULL,source varchar(12) NOT NULL,actor_id uuid,applied boolean NOT NULL DEFAULT false,created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX queued_jobs ON review_jobs(created_at) WHERE state IN ('QUEUED','PROCESSING');
CREATE TABLE service_flags(key text PRIMARY KEY,value boolean NOT NULL);
INSERT INTO service_flags VALUES ('index_ready',false);


CREATE TABLE integration_outbox(event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));
CREATE INDEX integration_outbox_pending ON integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE TABLE integration_inbox(event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());
