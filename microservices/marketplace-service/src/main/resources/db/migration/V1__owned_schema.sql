CREATE SCHEMA IF NOT EXISTS camerahub;
SET search_path TO camerahub,public;
CREATE TABLE brands (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(100) NOT NULL UNIQUE);
CREATE TABLE camera_models (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), brand_id uuid NOT NULL REFERENCES brands(id),
 model_name varchar(160) NOT NULL, device_type varchar(20) NOT NULL CHECK(device_type IN ('BODY','LENS','OTHER')),
 specs jsonb NOT NULL DEFAULT '{}'::jsonb, UNIQUE(brand_id,model_name,device_type)
);
CREATE TABLE products (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), seller_id uuid NOT NULL, model_id uuid NOT NULL REFERENCES camera_models(id),
 title varchar(240) NOT NULL, serial_number_private text, serial_last4 varchar(4), shutter_count integer CHECK(shutter_count>=0),
 price_vnd bigint NOT NULL CHECK(price_vnd>0), description text NOT NULL, condition_notes text NOT NULL,
 repair_history text, accessories text, listing_status varchar(24) NOT NULL DEFAULT 'DRAFT'
 CHECK(listing_status IN ('DRAFT','PENDING_REVIEW','NEEDS_REVISION','REJECTED','PUBLISHED','RESERVED','SOLD','HIDDEN')),
 inspection_badge varchar(20) NOT NULL DEFAULT 'NOT_INSPECTED'
 CHECK(inspection_badge IN ('NOT_INSPECTED','CERTIFIED','NOT_CERTIFIED','EXPIRED')),
 version integer NOT NULL DEFAULT 0 CHECK(version>=0), reviewed_by uuid, reviewed_at timestamptz,
 rejection_reason text, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE product_media (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
 storage_key text NOT NULL, media_type varchar(12) NOT NULL CHECK(media_type IN ('IMAGE','VIDEO')),
 sort_order integer NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE product_status_history (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 old_status text, new_status text NOT NULL, actor_id uuid, reason text,
 occurred_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE products DROP CONSTRAINT products_listing_status_check;
ALTER TABLE products ADD CONSTRAINT products_listing_status_check CHECK(listing_status IN ('DRAFT','AI_REVIEW','PENDING_REVIEW','NEEDS_REVISION','REJECTED','PUBLISHED','RESERVED','SOLD','HIDDEN'));
ALTER TABLE products ADD COLUMN presentation jsonb NOT NULL DEFAULT '{}'::jsonb,ADD COLUMN moderation_source varchar(12),ADD COLUMN moderation_note text,ADD COLUMN submitted_revision bigint NOT NULL DEFAULT 0,ADD COLUMN inspection_checks jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE product_media ADD COLUMN image_sha256 varchar(64),ADD COLUMN image_dhash bigint,ADD COLUMN image_signature text;
CREATE TABLE public_accounts(id uuid PRIMARY KEY,full_name varchar(160) NOT NULL,account_status varchar(20) NOT NULL,created_at timestamptz NOT NULL,profile_version bigint NOT NULL DEFAULT 0);
CREATE TABLE product_reservations(product_id uuid PRIMARY KEY REFERENCES products(id),batch_id uuid NOT NULL,buyer_id uuid NOT NULL,token uuid NOT NULL UNIQUE,snapshot jsonb NOT NULL,state varchar(20) NOT NULL CHECK(state IN ('HELD','RELEASED','CONFIRMED')),created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE reservation_batches(id uuid PRIMARY KEY,buyer_id uuid NOT NULL,product_ids jsonb NOT NULL,state varchar(20) NOT NULL DEFAULT 'HELD',created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX products_status ON products(listing_status,created_at);
CREATE INDEX products_seller ON products(seller_id,created_at);
CREATE INDEX media_product ON product_media(product_id,sort_order);



CREATE TABLE integration_outbox(event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));
CREATE INDEX integration_outbox_pending ON integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE TABLE integration_inbox(event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());
