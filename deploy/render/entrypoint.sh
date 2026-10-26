#!/bin/sh
set -eu
umask 077

fail() { printf '%s\n' "$1" >&2; exit 1; }
export SERVER_PORT="${PORT:-10000}"
export SERVICE_BIND_ADDRESS=0.0.0.0
export SESSION_COOKIE_SECURE="${SESSION_COOKIE_SECURE:-true}"
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
: "${SERVICE_INTERNAL_KEY:?SERVICE_INTERNAL_KEY is required}"

# Render supplies private hostname:port references, not HTTP URLs.
for domain in IDENTITY MARKETPLACE MODERATION COMMERCE ADVISOR; do
    eval "hostport=\${${domain}_HOSTPORT:-}"
    if [ -n "$hostport" ]; then
        case "$hostport" in *[!a-zA-Z0-9.:-]*) fail "Invalid private service address" ;; esac
        export "${domain}_URL=http://${hostport}"
    fi
done

if [ "$CAMERAHUB_SERVICE" != api-gateway ]; then
    : "${DB_HOST:?DB_HOST is required}" "${DB_PORT:?DB_PORT is required}"
    : "${DB_NAME:?DB_NAME is required}" "${DB_USER:?DB_USER is required}" "${DB_PASSWORD:?DB_PASSWORD is required}"
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    export SPRING_DATASOURCE_USERNAME="$DB_USER"
    export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
    : "${JWT_PUBLIC_KEY_B64:?JWT_PUBLIC_KEY_B64 is required}"
    mkdir -p /run/camerahub-keys
    printf '%s' "$JWT_PUBLIC_KEY_B64" | base64 -d > /run/camerahub-keys/public.pem
    chown 10001:10001 /run/camerahub-keys /run/camerahub-keys/public.pem
    export JWT_PUBLIC_KEY_PATH=/run/camerahub-keys/public.pem
fi

if [ "$CAMERAHUB_SERVICE" = identity-service ]; then
    : "${JWT_PRIVATE_KEY_B64:?JWT_PRIVATE_KEY_B64 is required}"
    : "${FRONTEND_URL:?FRONTEND_URL is required}"
    case "$FRONTEND_URL" in https://*) ;; *) fail "Public frontend URL must use HTTPS" ;; esac
    printf '%s' "$JWT_PRIVATE_KEY_B64" | base64 -d > /run/camerahub-keys/private.pem
    chown 10001:10001 /run/camerahub-keys/private.pem
    export JWT_PRIVATE_KEY_PATH=/run/camerahub-keys/private.pem
    export GOOGLE_REDIRECT_URI="${FRONTEND_URL%/}/api/auth/google/callback"
fi

if [ "$CAMERAHUB_SERVICE" = marketplace-service ]; then
    # Only this fixed Render disk path is initialized. Uploaded files run under UID 10001.
    mkdir -p /var/data/media
    chown 10001:10001 /var/data/media
    export CAMERAHUB_MEDIA_ROOT=/var/data/media
fi

unset JWT_PRIVATE_KEY_B64 JWT_PUBLIC_KEY_B64 DB_PASSWORD
exec su-exec 10001:10001 java -XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError -jar /app/app.jar
