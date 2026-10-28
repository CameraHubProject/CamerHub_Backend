CREATE TABLE camerahub.seller_reviews (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 order_id uuid NOT NULL UNIQUE REFERENCES camerahub.orders(id),
 rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comment text NOT NULL CHECK (char_length(btrim(comment)) BETWEEN 1 AND 2000),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_seller_reviews_created ON camerahub.seller_reviews(created_at DESC, id DESC);
