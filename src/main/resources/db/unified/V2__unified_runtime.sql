-- Add runtime tables/columns while retaining existing Neon accounts, listings and orders.

SET search_path TO camerahub,public;

CREATE TABLE IF NOT EXISTS camerahub.users (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), email text NOT NULL, password_hash text NOT NULL,
 full_name varchar(160) NOT NULL, phone varchar(20), account_status varchar(20) NOT NULL DEFAULT 'ACTIVE'
 CHECK (account_status IN ('ACTIVE','SUSPENDED','CLOSED')),
 email_verified_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.roles (code varchar(20) PRIMARY KEY CHECK (code IN ('BUYER','SELLER','INSPECTOR','ADMIN')));

CREATE TABLE IF NOT EXISTS camerahub.user_roles (
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 role_code varchar(20) NOT NULL REFERENCES roles(code), PRIMARY KEY(user_id,role_code)
);

CREATE TABLE IF NOT EXISTS camerahub.user_addresses (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES users(id),
 recipient_name varchar(160) NOT NULL, phone varchar(20) NOT NULL, province text NOT NULL, district text,
 ward text, street_address text NOT NULL, is_default boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.audit_logs (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), actor_id uuid REFERENCES users(id),
 entity_type varchar(80) NOT NULL, entity_id uuid NOT NULL, action varchar(80) NOT NULL,
 old_data jsonb, new_data jsonb, correlation_id text, occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.auth_identities (
 provider varchar(20) NOT NULL CHECK (provider = 'google'),
 subject varchar(255) NOT NULL,
 user_id uuid NOT NULL REFERENCES camerahub.users(id) ON DELETE CASCADE,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(provider, subject),
 UNIQUE(provider, user_id)
);

CREATE TABLE IF NOT EXISTS camerahub.spring_session (primary_id char(36) PRIMARY KEY,session_id char(36) NOT NULL UNIQUE,creation_time bigint NOT NULL,last_access_time bigint NOT NULL,max_inactive_interval integer NOT NULL,expiry_time bigint NOT NULL,principal_name varchar(100));

CREATE TABLE IF NOT EXISTS camerahub.spring_session_attributes (session_primary_id char(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,attribute_name varchar(200) NOT NULL,attribute_bytes bytea NOT NULL,PRIMARY KEY(session_primary_id,attribute_name));

CREATE TABLE IF NOT EXISTS camerahub.integration_outbox (event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));

CREATE TABLE IF NOT EXISTS camerahub.integration_inbox (event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.brands (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(100) NOT NULL UNIQUE);

CREATE TABLE IF NOT EXISTS camerahub.camera_models (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), brand_id uuid NOT NULL REFERENCES brands(id),
 model_name varchar(160) NOT NULL, device_type varchar(20) NOT NULL CHECK(device_type IN ('BODY','LENS','OTHER')),
 specs jsonb NOT NULL DEFAULT '{}'::jsonb, UNIQUE(brand_id,model_name,device_type)
);

CREATE TABLE IF NOT EXISTS camerahub.products (
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

CREATE TABLE IF NOT EXISTS camerahub.product_media (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
 storage_key text NOT NULL, media_type varchar(12) NOT NULL CHECK(media_type IN ('IMAGE','VIDEO')),
 sort_order integer NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.product_status_history (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 old_status text, new_status text NOT NULL, actor_id uuid, reason text,
 occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.public_accounts (id uuid PRIMARY KEY,full_name varchar(160) NOT NULL,account_status varchar(20) NOT NULL,created_at timestamptz NOT NULL,profile_version bigint NOT NULL DEFAULT 0);

CREATE TABLE IF NOT EXISTS camerahub.product_reservations (product_id uuid PRIMARY KEY REFERENCES products(id),batch_id uuid NOT NULL,buyer_id uuid NOT NULL,token uuid NOT NULL UNIQUE,snapshot jsonb NOT NULL,state varchar(20) NOT NULL CHECK(state IN ('HELD','RELEASED','CONFIRMED')),created_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.reservation_batches (id uuid PRIMARY KEY,buyer_id uuid NOT NULL,product_ids jsonb NOT NULL,state varchar(20) NOT NULL DEFAULT 'HELD',created_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL,
 buyer_id uuid NOT NULL, seller_id uuid NOT NULL,
 agreed_price_vnd bigint NOT NULL CHECK(agreed_price_vnd>0), shipping_fee_vnd bigint NOT NULL DEFAULT 0 CHECK(shipping_fee_vnd>=0),
 transaction_fee_vnd bigint NOT NULL DEFAULT 0 CHECK(transaction_fee_vnd=0),
 total_vnd bigint GENERATED ALWAYS AS (agreed_price_vnd + shipping_fee_vnd) STORED,
 product_snapshot jsonb NOT NULL, shipping_address_snapshot jsonb NOT NULL,
 order_status varchar(26) NOT NULL DEFAULT 'AWAITING_DEPOSIT' CHECK(order_status IN
 ('AWAITING_DEPOSIT','AWAITING_BALANCE','READY_TO_SHIP','SHIPPING','DELIVERED','PROTECTION_ACTIVE','DISPUTED','CANCELLATION_REVIEW','REFUND_PROCESSING','COMPLETED','CANCELLED','DELIVERY_EXCEPTION')),
 payment_deadline timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK(buyer_id<>seller_id)
);

CREATE TABLE IF NOT EXISTS camerahub.payment_records (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES orders(id),
 amount_vnd bigint NOT NULL CHECK(amount_vnd>=0), payment_mode varchar(20) NOT NULL DEFAULT 'SIMULATION' CHECK(payment_mode='SIMULATION'),
 status varchar(28) NOT NULL DEFAULT 'SIMULATED_PENDING' CHECK(status IN
 ('SIMULATED_PENDING','SIMULATED_PAID','SIMULATED_FAILED','SIMULATED_CANCELLED')),
 idempotency_key text NOT NULL UNIQUE, simulated_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.order_deposits (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL UNIQUE REFERENCES orders(id),
 deposit_rate_bps integer NOT NULL DEFAULT 1000 CHECK(deposit_rate_bps=1000),
 amount_vnd bigint NOT NULL CHECK(amount_vnd>0),
 status varchar(22) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PAID','APPLIED','REFUND_PENDING','REFUNDED','FORFEITED','CANCELLED')),
 paid_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.checkout_batches (id uuid PRIMARY KEY,buyer_id uuid NOT NULL,request_hash text NOT NULL,state varchar(20) NOT NULL DEFAULT 'PLACING',request jsonb NOT NULL,reservations jsonb NOT NULL DEFAULT '[]'::jsonb,last_error text,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.showrooms (id uuid PRIMARY KEY DEFAULT gen_random_uuid(),name varchar(160) NOT NULL,address text NOT NULL,active boolean NOT NULL DEFAULT true);

CREATE TABLE IF NOT EXISTS camerahub.buyback_requests (id uuid PRIMARY KEY DEFAULT gen_random_uuid(),user_id uuid NOT NULL,model varchar(160) NOT NULL,condition_notes text NOT NULL,expected_price_vnd bigint CHECK(expected_price_vnd>0),status varchar(24) NOT NULL DEFAULT 'PENDING',created_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.seller_reviews (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 order_id uuid NOT NULL UNIQUE REFERENCES camerahub.orders(id),
 rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comment text NOT NULL CHECK (char_length(btrim(comment)) BETWEEN 1 AND 2000),
 created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camerahub.review_jobs (listing_id uuid NOT NULL,revision bigint NOT NULL,owner_id uuid NOT NULL,snapshot jsonb NOT NULL,state varchar(24) NOT NULL DEFAULT 'QUEUED',note text,source varchar(12),claim_token uuid,leased_until timestamptz,attempts integer NOT NULL DEFAULT 0,decision_id uuid,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),PRIMARY KEY(listing_id,revision));

CREATE TABLE IF NOT EXISTS camerahub.listing_index (listing_id uuid PRIMARY KEY,revision bigint NOT NULL,snapshot jsonb NOT NULL,listing_status varchar(24) NOT NULL,updated_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.review_decisions (id uuid PRIMARY KEY,listing_id uuid NOT NULL,revision bigint NOT NULL,status varchar(24) NOT NULL,note text NOT NULL,source varchar(12) NOT NULL,actor_id uuid,applied boolean NOT NULL DEFAULT false,created_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.service_flags (key text PRIMARY KEY,value boolean NOT NULL);

CREATE TABLE IF NOT EXISTS camerahub.advisor_conversations (id uuid PRIMARY KEY,owner_key varchar(64) NOT NULL,messages jsonb NOT NULL DEFAULT '[]'::jsonb,lease_token uuid,leased_until timestamptz,updated_at timestamptz NOT NULL DEFAULT now());

CREATE TABLE IF NOT EXISTS camerahub.advisor_usage (day date NOT NULL,client_key varchar(64) NOT NULL,requests integer NOT NULL DEFAULT 0,PRIMARY KEY(day,client_key));

CREATE TABLE IF NOT EXISTS camerahub.advisor_rate_windows (client_key varchar(64) NOT NULL,minute bigint NOT NULL,requests integer NOT NULL DEFAULT 0,PRIMARY KEY(client_key,minute));

ALTER TABLE camerahub.users ADD COLUMN IF NOT EXISTS profile_version bigint NOT NULL DEFAULT 0;

ALTER TABLE camerahub.products ADD COLUMN IF NOT EXISTS submitted_revision bigint NOT NULL DEFAULT 0;

ALTER TABLE camerahub.products ADD COLUMN IF NOT EXISTS inspection_checks jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS state varchar(20) NOT NULL DEFAULT 'PLACING';

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS request jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS reservations jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS last_error text;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE camerahub.orders ADD COLUMN IF NOT EXISTS release_synced boolean NOT NULL DEFAULT false;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS leased_until timestamptz;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS lease_token uuid;

ALTER TABLE camerahub.checkout_batches ADD COLUMN IF NOT EXISTS reservation_synced boolean NOT NULL DEFAULT false;

INSERT INTO camerahub.roles(code) VALUES ('BUYER'),('SELLER'),('ADMIN') ON CONFLICT DO NOTHING;

INSERT INTO camerahub.service_flags(key,value) VALUES ('index_ready',false) ON CONFLICT DO NOTHING;

ALTER TABLE camerahub.orders DROP CONSTRAINT IF EXISTS orders_deposit_rate_bps_check;

ALTER TABLE camerahub.orders ADD CONSTRAINT orders_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));

ALTER TABLE camerahub.orders ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;

ALTER TABLE camerahub.order_deposits DROP CONSTRAINT IF EXISTS order_deposits_deposit_rate_bps_check;

ALTER TABLE camerahub.order_deposits ADD CONSTRAINT order_deposits_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));

ALTER TABLE camerahub.order_deposits ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;

ALTER TABLE camerahub.products DROP CONSTRAINT IF EXISTS products_listing_status_check;

ALTER TABLE camerahub.products ADD CONSTRAINT products_listing_status_check CHECK(listing_status IN ('DRAFT','AI_REVIEW','PENDING_REVIEW','NEEDS_REVISION','REJECTED','PUBLISHED','RESERVED','SOLD','HIDDEN'));

CREATE TABLE IF NOT EXISTS camerahub.jwt_sessions (
 id uuid PRIMARY KEY,
 user_id uuid NOT NULL REFERENCES camerahub.users(id),
 refresh_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL,
 revoked boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS jwt_sessions_user ON camerahub.jwt_sessions(user_id);



INSERT INTO camerahub.public_accounts(id,full_name,account_status,created_at,profile_version)
SELECT id,full_name,account_status,created_at,profile_version FROM camerahub.users
ON CONFLICT(id) DO UPDATE SET full_name=excluded.full_name,account_status=excluded.account_status,profile_version=excluded.profile_version
WHERE camerahub.public_accounts.profile_version<=excluded.profile_version;

-- Reconnect pre-existing orders to the unified checkout/reservation model without changing their amounts or statuses.
INSERT INTO camerahub.checkout_batches(id,buyer_id,request_hash,state,request,reservation_synced)
SELECT id,buyer_id,'legacy','PLACED','{}'::jsonb,true FROM camerahub.orders WHERE checkout_batch_id IS NULL
ON CONFLICT DO NOTHING;
UPDATE camerahub.orders SET checkout_batch_id=id WHERE checkout_batch_id IS NULL;
UPDATE camerahub.checkout_batches batch SET state='PLACED',reservation_synced=true
WHERE EXISTS(SELECT 1 FROM camerahub.orders purchase WHERE purchase.checkout_batch_id=batch.id);
INSERT INTO camerahub.reservation_batches(id,buyer_id,product_ids,state)
SELECT checkout_batch_id,buyer_id,jsonb_agg(product_id ORDER BY product_id),'CONFIRMED'
FROM camerahub.orders WHERE order_status<>'CANCELLED' GROUP BY checkout_batch_id,buyer_id ON CONFLICT DO NOTHING;
INSERT INTO camerahub.product_reservations(product_id,batch_id,buyer_id,token,snapshot,state)
SELECT product_id,checkout_batch_id,buyer_id,gen_random_uuid(),product_snapshot,'CONFIRMED'
FROM camerahub.orders WHERE order_status<>'CANCELLED' ON CONFLICT DO NOTHING;


-- A new installation needs the same brand choices as the local imported catalog.
-- Seed only an empty catalog: existing admin edits and deletions remain authoritative.
INSERT INTO camerahub.brands(name)
SELECT name FROM (VALUES
 ('Canon'), ('Sony'), ('Nikon'), ('Fujifilm'), ('Panasonic'), ('Olympus'), ('OM System'), ('Pentax'),
 ('Ricoh'), ('Leica'), ('Kodak'), ('Casio'), ('Samsung'), ('Minolta'), ('Hasselblad'), ('Sigma'),
 ('GoPro'), ('DJI'), ('Insta360'), ('Polaroid'), ('Phase One'), ('Mamiya'), ('Yashica'), ('Konica'),
 ('Rollei'), ('Voigtländer'), ('Lomography'), ('Blackmagic Design'), ('RED'), ('ARRI'), ('Z CAM'),
 ('Kinefinity'), ('AKASO'), ('SJCAM'), ('Khác')
) AS initial_brands(name)
WHERE NOT EXISTS (SELECT 1 FROM camerahub.brands);


CREATE INDEX IF NOT EXISTS unified_outbox_pending ON camerahub.integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;

CREATE INDEX IF NOT EXISTS unified_queued_jobs ON camerahub.review_jobs(created_at) WHERE state IN ('QUEUED','PROCESSING');

CREATE INDEX IF NOT EXISTS unified_reviews_created ON camerahub.seller_reviews(created_at DESC,id DESC);
