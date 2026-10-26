CREATE SCHEMA IF NOT EXISTS camerahub;
SET search_path TO camerahub,public;
CREATE TABLE orders (
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
CREATE TABLE payment_records (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES orders(id),
 amount_vnd bigint NOT NULL CHECK(amount_vnd>=0), payment_mode varchar(20) NOT NULL DEFAULT 'SIMULATION' CHECK(payment_mode='SIMULATION'),
 status varchar(28) NOT NULL DEFAULT 'SIMULATED_PENDING' CHECK(status IN
 ('SIMULATED_PENDING','SIMULATED_PAID','SIMULATED_FAILED','SIMULATED_CANCELLED')),
 idempotency_key text NOT NULL UNIQUE, simulated_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE orders ADD COLUMN deposit_rate_bps integer NOT NULL DEFAULT 1000 CHECK(deposit_rate_bps=1000);
ALTER TABLE orders ADD COLUMN deposit_amount_vnd bigint GENERATED ALWAYS AS ((agreed_price_vnd * deposit_rate_bps + 5000) / 10000) STORED;
ALTER TABLE orders ADD COLUMN balance_product_amount_vnd bigint GENERATED ALWAYS AS (agreed_price_vnd - ((agreed_price_vnd * deposit_rate_bps + 5000) / 10000)) STORED;
ALTER TABLE orders ADD COLUMN deposit_deadline timestamptz;
ALTER TABLE orders ADD COLUMN balance_deadline timestamptz;
CREATE TABLE order_deposits (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL UNIQUE REFERENCES orders(id),
 deposit_rate_bps integer NOT NULL DEFAULT 1000 CHECK(deposit_rate_bps=1000),
 amount_vnd bigint NOT NULL CHECK(amount_vnd>0),
 status varchar(22) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PAID','APPLIED','REFUND_PENDING','REFUNDED','FORFEITED','CANCELLED')),
 paid_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE payment_records ADD COLUMN payment_stage varchar(24) NOT NULL DEFAULT 'DEPOSIT'
 CHECK(payment_stage IN ('DEPOSIT','BALANCE_AND_SHIPPING'));
ALTER TABLE payment_records ADD CONSTRAINT chk_payment_positive CHECK(amount_vnd>0);
CREATE UNIQUE INDEX uq_paid_per_payment_stage ON payment_records(order_id,payment_stage) WHERE status='SIMULATED_PAID';
CREATE INDEX idx_payments_order_stage ON payment_records(order_id,payment_stage,created_at DESC);


ALTER TABLE orders DROP CONSTRAINT orders_deposit_rate_bps_check;
ALTER TABLE orders ADD CONSTRAINT orders_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));
ALTER TABLE orders ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;
ALTER TABLE order_deposits DROP CONSTRAINT order_deposits_deposit_rate_bps_check;
ALTER TABLE order_deposits ADD CONSTRAINT order_deposits_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));
ALTER TABLE order_deposits ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;
CREATE TABLE checkout_batches(id uuid PRIMARY KEY,buyer_id uuid NOT NULL,request_hash text NOT NULL,state varchar(20) NOT NULL DEFAULT 'PLACING',request jsonb NOT NULL,reservations jsonb NOT NULL DEFAULT '[]'::jsonb,last_error text,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now());
ALTER TABLE orders ADD COLUMN checkout_batch_id uuid REFERENCES checkout_batches(id);
ALTER TABLE orders ADD COLUMN release_synced boolean NOT NULL DEFAULT false;
ALTER TABLE checkout_batches ADD COLUMN leased_until timestamptz,ADD COLUMN lease_token uuid,ADD COLUMN reservation_synced boolean NOT NULL DEFAULT false;
CREATE UNIQUE INDEX live_order_product ON orders(product_id) WHERE order_status<>'CANCELLED';
CREATE TABLE showrooms(id uuid PRIMARY KEY DEFAULT gen_random_uuid(),name varchar(160) NOT NULL,address text NOT NULL,active boolean NOT NULL DEFAULT true);
CREATE TABLE buyback_requests(id uuid PRIMARY KEY DEFAULT gen_random_uuid(),user_id uuid NOT NULL,model varchar(160) NOT NULL,condition_notes text NOT NULL,expected_price_vnd bigint CHECK(expected_price_vnd>0),status varchar(24) NOT NULL DEFAULT 'PENDING',created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE camerahub.seller_reviews (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 order_id uuid NOT NULL UNIQUE REFERENCES camerahub.orders(id),
 rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comment text NOT NULL CHECK (char_length(btrim(comment)) BETWEEN 1 AND 2000),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_seller_reviews_created ON camerahub.seller_reviews(created_at DESC, id DESC);


CREATE TABLE integration_outbox(event_id uuid NOT NULL,target text NOT NULL,payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),attempts integer NOT NULL DEFAULT 0,next_attempt_at timestamptz NOT NULL DEFAULT now(),leased_until timestamptz,lease_token uuid,delivered_at timestamptz,PRIMARY KEY(event_id,target));
CREATE INDEX integration_outbox_pending ON integration_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE TABLE integration_inbox(event_id uuid PRIMARY KEY,event_type text NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());
