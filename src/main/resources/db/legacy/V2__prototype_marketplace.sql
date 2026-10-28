-- Additive support for the EXE CP3 prototype. V1 and existing records are preserved.
ALTER TABLE camerahub.products ADD COLUMN presentation jsonb NOT NULL DEFAULT '{}'::jsonb;
-- New prototype orders use 30%. Retain the rate and computed amounts on legacy 10% orders.
ALTER TABLE camerahub.orders DROP CONSTRAINT orders_deposit_rate_bps_check;
ALTER TABLE camerahub.orders ADD CONSTRAINT orders_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));
ALTER TABLE camerahub.orders ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;
ALTER TABLE camerahub.order_deposits DROP CONSTRAINT order_deposits_deposit_rate_bps_check;
ALTER TABLE camerahub.order_deposits ADD CONSTRAINT order_deposits_deposit_rate_bps_check CHECK(deposit_rate_bps IN (1000,3000));
ALTER TABLE camerahub.order_deposits ALTER COLUMN deposit_rate_bps SET DEFAULT 3000;
CREATE TABLE camerahub.checkout_batches (
 id uuid PRIMARY KEY, buyer_id uuid NOT NULL REFERENCES camerahub.users(id),
 request_hash text NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE camerahub.orders ADD COLUMN checkout_batch_id uuid REFERENCES camerahub.checkout_batches(id);
CREATE TABLE camerahub.showrooms (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(160) NOT NULL,
 address text NOT NULL, active boolean NOT NULL DEFAULT true
);
-- Showroom locations must be configured by the operator; no invented real addresses.
CREATE TABLE camerahub.buyback_requests (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES camerahub.users(id),
 model varchar(160) NOT NULL, condition_notes text NOT NULL, expected_price_vnd bigint CHECK(expected_price_vnd>0),
 status varchar(24) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','CONTACTED','CLOSED')),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_buyback_user ON camerahub.buyback_requests(user_id,created_at DESC);
