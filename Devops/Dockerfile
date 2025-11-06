# Dockerfile for PostgreSQL with locale configuration
# Author: [Juan Pablo Saavedra]
# Date: [06-11-2025]

FROM postgres:latest

# Configure locales
RUN localedef -i es_ES -c -f UTF-8 -A /usr/share/locale/locale.alias es_ES.UTF-8

# Set environment variable
ENV LANG es_ES.UTF-8