# Documentación de Puertos de Base de Datos - SGH

## Resumen de Configuración de Ambientes

Este documento detalla la configuración de puertos para las bases de datos en cada ambiente del proyecto SGH (Sistema de Gestión de Horarios). Actualmente, Develop usa MySQL mientras que QA, Staging y Producción usan PostgreSQL.

---

## 📊 Tabla de Puertos por Ambiente

| Ambiente | Puerto Host | Puerto Contenedor | Nombre Base de Datos | Usuario | Contenedor |
|----------|-------------|-------------------|---------------------|---------|------------|
| **Develop** | `3307` | `3306` | `horarios` | `user` | `DB_Develop` |
| **QA** | `5433` | `5432` | `DB_SGH_QA` | `sgh_user` | `sgh-postgres-qa` |
| **Staging** | `5434` | `5432` | `DB_SGH_Staging` | `sgh_user` | `sgh-postgres-staging` |
| **Production** | `5435` | `5432` | `DB_SGH_Production` | `sgh_user` | `sgh-postgres-prod` |

---

## 🔧 Detalles de Configuración por Ambiente

### 1. Ambiente de Desarrollo (Develop)
- **Puerto de acceso:** `3307`
- **Base de datos:** `horarios`
- **Usuario:** `user`
- **Archivo de configuración:** `Devops/develop/.env.dev`
- **Docker Compose:** `Devops/docker-compose-databases.yml` (centralizado)
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 3307
  Database: horarios
  User: user
  Password: [ver .env.dev]
  ```

### 2. Ambiente de QA
- **Puerto de acceso:** `5433`
- **Base de datos:** `DB_SGH_QA`
- **Usuario:** `sgh_user`
- **Archivo de configuración:** `Devops/qa/.env.qa`
- **Docker Compose:** `Devops/Docker-Compose.yml` (centralizado)
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 5433
  Database: DB_SGH_QA
  User: sgh_user
  Password: [ver .env.qa]
  ```

### 3. Ambiente de Staging
- **Puerto de acceso:** `5434`
- **Base de datos:** `DB_SGH_Staging`
- **Usuario:** `sgh_user`
- **Archivo de configuración:** `Devops/staging/.env.staging`
- **Docker Compose:** `Devops/Docker-Compose.yml` (centralizado)
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 5434
  Database: DB_SGH_Staging
  User: sgh_user
  Password: [ver .env.staging]
  ```

### 4. Ambiente de Producción (Production)
- **Puerto de acceso:** `5435`
- **Base de datos:** `DB_SGH_Production`
- **Usuario:** `sgh_user`
- **Archivo de configuración:** `Devops/prod/.env.prod`
- **Docker Compose:** `Devops/Docker-Compose.yml` (centralizado)
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 5435
  Database: DB_SGH_Production
  User: sgh_user
  Password: [ver .env.prod]
  ```

---

## 🚀 Comandos para Levantar las Bases de Datos

**IMPORTANTE:** Todos los comandos se ejecutan desde la carpeta `Devops/` ya que hay un único Docker-Compose.yml centralizado.

### Levantar todos los ambientes:

```bash
cd Devops
docker-compose up -d
```

### Levantar un ambiente específico:

```bash
cd Devops

# Solo Develop
docker-compose up -d postgres-develop

# Solo QA
docker-compose up -d postgres-qa

# Solo Staging
docker-compose up -d postgres-staging

# Solo Production
docker-compose up -d postgres-prod
```

### Verificar el estado de los contenedores:

```bash
docker ps | grep sgh-postgres
```

### Detener todos los ambientes:

```bash
cd Devops
docker-compose down
```

### Detener un ambiente específico:

```bash
cd Devops

# Solo Develop
docker-compose stop postgres-develop

# Solo QA
docker-compose stop postgres-qa

# Solo Staging
docker-compose stop postgres-staging

# Solo Production
docker-compose stop postgres-prod
```

---

## 🔍 Verificación de Conectividad

### Verificar que la base de datos está corriendo:

```bash
# Develop (MySQL)
docker exec -it DB_Develop mysqladmin ping -h localhost

# QA (PostgreSQL)
docker exec -it sgh-postgres-qa pg_isready -U postgres

# Staging
docker exec -it sgh-postgres-staging pg_isready -U postgres

# Production
docker exec -it sgh-postgres-prod pg_isready -U postgres
```

### Conectarse a la base de datos desde el contenedor:

```bash
# Develop (MySQL)
docker exec -it DB_Develop mysql -u user -p horarios

# QA (PostgreSQL)
docker exec -it sgh-postgres-qa psql -U sgh_user -d DB_SGH_QA

# Staging
docker exec -it sgh-postgres-staging psql -U sgh_user -d DB_SGH_Staging

# Production
docker exec -it sgh-postgres-prod psql -U sgh_user -d DB_SGH_Production
```

---

## 📝 Configuración del Backend

El backend de Spring Boot debe configurarse para conectarse a cada ambiente según corresponda:

### application.properties (ejemplo para Develop):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/horarios
spring.datasource.username=user
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Variables de entorno por ambiente:

- **Develop:** `DB_PORT=3306`
- **QA:** `DB_PORT=5433`
- **Staging:** `DB_PORT=5434`
- **Production:** `DB_PORT=5435`

---

## 🔐 Seguridad

- Las contraseñas están almacenadas en los archivos `.env.*` de cada ambiente
- **IMPORTANTE:** Los archivos `.env.*` deben estar en `.gitignore` y no deben ser commiteados al repositorio
- Para producción, se recomienda usar secretos de Jenkins o variables de entorno seguras

---

## 📦 Volúmenes y Backups

Cada ambiente tiene su propio volumen persistente:

- **Develop:** `mysql_data_develop`
- **QA:** `postgres_data_qa`
- **Staging:** `postgres_data_staging`
- **Production:** `postgres_data_prod`

Los backups se almacenan en:
- `Devops/develop/backups/develop/`
- `Devops/qa/backups/qa/`
- `Devops/staging/backups/staging/`
- `Devops/prod/backups/prod/`

---

## 🌐 Redes Docker

Cada ambiente tiene su propia red aislada:

- **Develop:** `network_develop`
- **QA:** `network_qa`
- **Staging:** `network_staging`
- **Production:** `network_prod`

---

## ⚠️ Notas Importantes

1. **Puertos únicos:** Cada ambiente usa un puerto diferente para evitar conflictos
2. **Locale:** Las bases de datos PostgreSQL están configuradas con locale `es_ES.UTF-8`, MySQL con `utf8mb4`
3. **Health checks:** Cada contenedor tiene configurado un health check para verificar su estado
4. **Restart policy:** Todos los contenedores están configurados con `restart: always`
5. **Jenkins:** Esta configuración es compatible con pipelines de Jenkins para CI/CD

---

## 📞 Soporte

Para problemas o dudas sobre la configuración de las bases de datos, contactar al equipo de DevOps.

**Última actualización:** 2025-11-28
**Versión:** 1.1.0