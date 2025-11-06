# Dockerfile for PostgreSQL with locale configuration
# Author: [Juan Pablo Saavedra]
# Date: [06-11-2025]

FROM postgres:16

# Install locales if not available, ignore errors
RUN apt-get update && apt-get install -y locales && \
    localedef -i es_ES -c -f UTF-8 -A /usr/share/locale/locale.alias es_ES.UTF-8 || true && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Set environment variables for PostgreSQL (will be overridden by docker-compose)
ENV LANG es_ES.UTF-8
ENV POSTGRES_INITDB_ARGS="--encoding=UTF-8 --lc-collate=es_ES.UTF-8 --lc-ctype=es_ES.UTF-8"