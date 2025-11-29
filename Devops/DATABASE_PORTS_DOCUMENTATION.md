# Documentación de Puertos de Base de Datos MySQL - SGH

## Resumen de Configuración de Ambientes

Este documento detalla la configuración de puertos para las bases de datos PostgreSQL en cada ambiente del proyecto SGH (Sistema de Gestión de Horarios).

---

## 📊 Tabla de Puertos por Ambiente

| Ambiente | Puerto Host | Puerto Contenedor | Nombre Base de Datos | Usuario | Contenedor | Motor |
|----------|-------------|-------------------|---------------------|---------|------------|--------|
| **Develop** | `3307` | `3306` | `DB_SGH_Develop` | `sgh_user` | `mysql-develop` | MySQL |
| **QA** | `3308` | `3306` | `DB_SGH_QA` | `sgh_user` | `mysql-qa` | MySQL |
| **Staging** | `3309` | `3306` | `DB_SGH_Staging` | `sgh_user` | `mysql-staging` | MySQL |
| **Production** | `3310` | `3306` | `DB_SGH_Production` | `sgh_user` | `mysql-prod` | MySQL |

---

## 🔧 Detalles de Configuración por Ambiente

### 1. Ambiente de Desarrollo (Develop)
- **Puerto de acceso:** `5432`
- **Base de datos:** `DB_SGH_Develop`
- **Usuario:** `sgh_user`
- **Archivo de configuración:** `Devops/develop/.env.dev`
- **Docker Compose:** `Devops/Docker-Compose.yml` (centralizado)
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 5432
  Database: DB_SGH_Develop
  User: sgh_user
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
- **Puerto de acceso:** `3310`
- **Base de datos:** `DB_SGH_Production`
- **Usuario:** `sgh_user`
- **Motor:** MySQL 8.0
- **Archivo de configuración:** `Devops/prod/.env.prod`
- **Docker Compose:** `Devops/docker-compose-databases.yml`
- **Conexión desde host:**
  ```
  Host: localhost
  Port: 3310
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

### Verificar que MySQL está corriendo:

```bash
# Develop
docker exec -it mysql-develop mysqladmin ping -h localhost

# QA
docker exec -it mysql-qa mysqladmin ping -h localhost

# Staging
docker exec -it mysql-staging mysqladmin ping -h localhost

# Production
docker exec -it mysql-prod mysqladmin ping -h localhost
```

### Conectarse a la base de datos desde el contenedor:

```bash
# Develop
docker exec -it mysql-develop mysql -u sgh_user -p DB_SGH_Develop

# QA
docker exec -it mysql-qa mysql -u sgh_user -p DB_SGH_QA

# Staging
docker exec -it mysql-staging mysql -u sgh_user -p DB_SGH_Staging

# Production
docker exec -it mysql-prod mysql -u sgh_user -p DB_SGH_Production
```

---

## 📝 Configuración del Backend

El backend de Spring Boot debe configurarse para conectarse a cada ambiente según corresponda:

### application.properties (ejemplo para Develop):

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/DB_SGH_Develop
spring.datasource.username=sgh_user
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

### Variables de entorno por ambiente:

- **Develop:** `DB_PORT=3307`
- **QA:** `DB_PORT=3308`
- **Staging:** `DB_PORT=3309`
- **Production:** `DB_PORT=3310`

---

## 🔐 Seguridad

- Las contraseñas están almacenadas en los archivos `.env.*` de cada ambiente
- **IMPORTANTE:** Los archivos `.env.*` deben estar en `.gitignore` y no deben ser commiteados al repositorio
- Para producción, se recomienda usar secretos de Jenkins o variables de entorno seguras

---

## 📦 Volúmenes y Backups

Cada ambiente tiene su propio volumen persistente:

- **Develop:** `postgres_data_develop`
- **QA:** `postgres_data_qa`
- **Staging:** `postgres_data_staging`
- **Production:** `postgres_data_prod`

Los backups se almacenan en:
- `Devops/backups/develop/`
- `Devops/backups/qa/`
- `Devops/backups/staging/`
- `Devops/backups/prod/`

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
2. **Motor de base de datos:** Todos los ambientes usan MySQL 8.0
3. **Configuración MySQL:** Todas las bases de datos están configuradas con UTF-8 y collation unicode
4. **Health checks:** Cada contenedor tiene configurado un health check para verificar su estado
5. **Restart policy:** Todos los contenedores están configurados con `restart: always`
6. **Jenkins:** Esta configuración es compatible con pipelines de Jenkins para CI/CD

---

## 📞 Soporte

Para problemas o dudas sobre la configuración de las bases de datos, contactar al equipo de DevOps.

**Última actualización:** 2025-01-06
**Versión:** 1.0.0