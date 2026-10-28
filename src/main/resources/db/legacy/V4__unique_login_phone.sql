-- Preserve existing phone values; canonicalize only the unique sign-in key.
CREATE FUNCTION camerahub.normalized_login_phone(value text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
 SELECT CASE
   WHEN number ~ '^0[35789][0-9]{8}$' THEN '+84' || substring(number FROM 2)
   WHEN number ~ '^84[35789][0-9]{8}$' THEN '+' || number
   WHEN number ~ '^\+84[35789][0-9]{8}$' THEN number
   ELSE NULL
 END FROM (SELECT regexp_replace(value, '[[:space:]().-]', '', 'g') AS number) cleaned
$$;

-- Also prevents duplicates inserted concurrently or via another writer.
-- Existing duplicates must be resolved by their owners; never discard/merge accounts.
CREATE UNIQUE INDEX uq_users_login_phone
 ON camerahub.users(camerahub.normalized_login_phone(phone))
 WHERE camerahub.normalized_login_phone(phone) IS NOT NULL;
