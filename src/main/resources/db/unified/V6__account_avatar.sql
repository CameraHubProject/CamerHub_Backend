ALTER TABLE camerahub.users ADD COLUMN avatar_key uuid;
ALTER TABLE camerahub.users ADD COLUMN avatar_content bytea;
ALTER TABLE camerahub.users ADD COLUMN avatar_type varchar(20);
ALTER TABLE camerahub.users ADD CONSTRAINT valid_avatar CHECK (
    (avatar_key IS NULL AND avatar_content IS NULL AND avatar_type IS NULL) OR
    (avatar_key IS NOT NULL AND avatar_content IS NOT NULL AND octet_length(avatar_content) BETWEEN 1 AND 2097152 AND avatar_type IS NOT NULL AND avatar_type IN ('image/jpeg','image/png','image/webp'))
);
