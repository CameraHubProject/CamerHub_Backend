#!/bin/sh
set -eu
umask 077
missing=""
for setting in DB_URL DB_USERNAME DB_PASSWORD JWT_PUBLIC_KEY_B64 JWT_PRIVATE_KEY_B64 SERVICE_INTERNAL_KEY FRONTEND_URL; do
    eval "value=\${${setting}:-}"
    [ -n "$value" ] || missing="$missing $setting"
done
if [ -n "$missing" ]; then printf 'Missing required environment variables:%s\n' "$missing" >&2; exit 1; fi
case "$DB_URL" in jdbc:postgresql://*) ;; *) echo 'DB_URL must be a PostgreSQL JDBC URL' >&2; exit 1 ;; esac
case "$FRONTEND_URL" in https://*) ;; *) echo 'FRONTEND_URL must use HTTPS' >&2; exit 1 ;; esac
export PORT="${PORT:-10000}"
export SERVICE_BIND_ADDRESS=0.0.0.0
export SESSION_COOKIE_SECURE="${SESSION_COOKIE_SECURE:-true}"
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
export GOOGLE_REDIRECT_URI="${FRONTEND_URL%/}/api/auth/google/callback"
mkdir -p /run/camerahub-keys
printf '%s' "$JWT_PUBLIC_KEY_B64" | base64 -d > /run/camerahub-keys/public.pem
printf '%s' "$JWT_PRIVATE_KEY_B64" | base64 -d > /run/camerahub-keys/private.pem
chown -R 10001:10001 /run/camerahub-keys
export JWT_PUBLIC_KEY_PATH=/run/camerahub-keys/public.pem
export JWT_PRIVATE_KEY_PATH=/run/camerahub-keys/private.pem
unset JWT_PRIVATE_KEY_B64 JWT_PUBLIC_KEY_B64
exec su-exec 10001:10001 java -XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError -jar /app/app.jar
