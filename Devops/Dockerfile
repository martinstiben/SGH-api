# Dockerfile para PostgreSQL 16 con configuración de locale en español
FROM postgres:16-alpine

# Establecer variables de entorno para el locale en español
ENV LANG=es_ES.UTF-8
ENV LC_ALL=es_ES.UTF-8
ENV POSTGRES_INITDB_ARGS=--encoding=UTF-8 --locale=C

# Copiar script de inicialización personalizado
COPY Devops/init.sql /docker-entrypoint-initdb.d/

# Exponer puerto por defecto de PostgreSQL
EXPOSE 5432

# Comando por defecto
CMD ["postgres"]