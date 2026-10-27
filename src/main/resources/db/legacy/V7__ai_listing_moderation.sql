ALTER TABLE camerahub.products DROP CONSTRAINT products_listing_status_check;
ALTER TABLE camerahub.products ADD CONSTRAINT products_listing_status_check
 CHECK (listing_status IN ('DRAFT','AI_REVIEW','PENDING_REVIEW','NEEDS_REVISION','REJECTED','PUBLISHED','RESERVED','SOLD','HIDDEN'));
ALTER TABLE camerahub.products
 ADD COLUMN moderation_source varchar(12),
 ADD COLUMN moderation_note text,
 ADD COLUMN moderation_started_at timestamptz;
ALTER TABLE camerahub.product_media
 ADD COLUMN image_sha256 varchar(64),
 ADD COLUMN image_dhash bigint,
 ADD COLUMN image_signature text;
CREATE INDEX idx_media_image_sha ON camerahub.product_media(image_sha256);
CREATE INDEX idx_products_ai_queue ON camerahub.products(created_at) WHERE listing_status='AI_REVIEW';
-- Existing submissions enter AI review once; drafts stay private until explicitly submitted.
UPDATE camerahub.products SET listing_status='AI_REVIEW',moderation_note='Đang chờ AI kiểm tra'
 WHERE listing_status='PENDING_REVIEW';
