ALTER TABLE camerahub.products ADD COLUMN deleted_at timestamptz;
ALTER TABLE camerahub.products ADD COLUMN withdrawal_reason text;
ALTER TABLE camerahub.products ADD CONSTRAINT valid_withdrawal_reason CHECK (
    withdrawal_reason IS NULL OR (length(btrim(withdrawal_reason)) BETWEEN 1 AND 2000)
);
ALTER TABLE camerahub.products ADD CONSTRAINT deleted_listing_is_hidden CHECK (
    deleted_at IS NULL OR (listing_status = 'HIDDEN' AND withdrawal_reason IS NOT NULL)
);
