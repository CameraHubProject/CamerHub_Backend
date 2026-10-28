CREATE TABLE IF NOT EXISTS camerahub.product_media_data (
 media_id uuid PRIMARY KEY REFERENCES camerahub.product_media(id) ON DELETE CASCADE,
 content bytea NOT NULL CHECK(octet_length(content)<=20971520),
 created_at timestamptz NOT NULL DEFAULT now()
);
