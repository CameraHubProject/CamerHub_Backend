-- Run this entire file in the Neon SQL Editor for the CameraHub database.
-- Creates a new timestamped backup schema on each run; does not modify live data.
-- Includes password hashes and private profile data. Keep this schema private.
-- JWT tokens and browser sessions are intentionally excluded from user backups.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;

DO $backup$
DECLARE
    backup_schema text := 'user_backup_' || to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYYMMDD_HH24MISS_US');
    table_name text;
    row_count bigint;
BEGIN
    -- Fail before creating a backup if any expected source table is missing.
    FOREACH table_name IN ARRAY ARRAY[
        'roles', 'users', 'user_roles', 'user_addresses',
        'auth_identities', 'seller_verifications', 'public_accounts'
    ] LOOP
        IF to_regclass(format('camerahub.%I', table_name)) IS NULL THEN
            RAISE EXCEPTION 'Source table camerahub.% is missing', table_name;
        END IF;
    END LOOP;

    EXECUTE format('CREATE SCHEMA %I', backup_schema);
    EXECUTE format('REVOKE ALL ON SCHEMA %I FROM PUBLIC', backup_schema);
    EXECUTE format(
        'CREATE TABLE %I.backup_manifest (source_table text PRIMARY KEY, row_count bigint NOT NULL, backed_up_at timestamptz NOT NULL DEFAULT transaction_timestamp())',
        backup_schema
    );

    FOREACH table_name IN ARRAY ARRAY[
        'roles', 'users', 'user_roles', 'user_addresses',
        'auth_identities', 'seller_verifications', 'public_accounts'
    ] LOOP
        EXECUTE format('CREATE TABLE %I.%I AS TABLE camerahub.%I', backup_schema, table_name, table_name);
        GET DIAGNOSTICS row_count = ROW_COUNT;
        EXECUTE format('REVOKE ALL ON TABLE %I.%I FROM PUBLIC', backup_schema, table_name);
        EXECUTE format('INSERT INTO %I.backup_manifest(source_table, row_count) VALUES ($1, $2)', backup_schema)
            USING table_name, row_count;
        RAISE NOTICE 'Backed up camerahub.%: % rows', table_name, row_count;
    END LOOP;

    EXECUTE format('REVOKE ALL ON TABLE %I.backup_manifest FROM PUBLIC', backup_schema);
    RAISE NOTICE 'Backup complete: schema %. Inspect %.backup_manifest for row counts.', backup_schema, backup_schema;
END
$backup$;

COMMIT;

-- List snapshots created by this script:
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name LIKE 'user\_backup\_%' ESCAPE '\'
ORDER BY schema_name DESC;

-- This is a data snapshot in the same Neon database, not an offsite backup.
-- It contains no foreign keys/indexes and is meant as a source for controlled restore.
-- Run BEFORE resetting/deleting users. Running after reset backs up the empty tables.
