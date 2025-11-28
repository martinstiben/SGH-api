# Sistema de Gestión de Horarios (SGH)

## 🚀 Inicio Rápido con Docker

### Prerrequisitos
- Docker
- Docker Compose

### Ejecutar la aplicación completa

```bash
# Construir e iniciar todos los servicios
docker-compose up --build

# O ejecutar en segundo plano
docker-compose up -d --build
```

### Servicios incluidos
- **MySQL 8.0**: Base de datos en puerto 3306
- **Spring Boot App**: API REST en puerto 8082

### Acceder a la aplicación
- **API**: http://localhost:8082
- **Swagger UI**: http://localhost:8082/swagger-ui/index.html
- **Actuator Health**: http://localhost:8082/actuator/health

## 📧 Sistema de Notificaciones

### Notificaciones Automáticas
Cuando se asigna un horario, el sistema envía automáticamente:
- **1 notificación In-App** al profesor
- **1 email** al profesor
- **1 notificación In-App** a cada coordinador
- **1 email** a cada coordinador

### Endpoints de Notificaciones
- `POST /api/notifications/send` - Enviar notificación individual
- `POST /api/notifications/send/bulk` - Enviar notificaciones masivas
- `POST /api/notifications/send/role/{role}` - Enviar a rol específico
- `POST /api/notifications/retry-failed` - Reintentar notificaciones fallidas

### Notificaciones In-App
- `GET /api/in-app-notifications/active` - Notificaciones activas
- `GET /api/in-app-notifications/unread` - Notificaciones no leídas
- `PUT /api/in-app-notifications/{id}/read` - Marcar como leída

## 🔧 Configuración

### Variables de Entorno
```bash
# Base de datos (configurado en docker-compose.yml)
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/horarios
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=userpass

# Email (opcional, configurado en application.properties)
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password
```

### Ejecutar sin Docker (desarrollo local)
```bash
# Asegurarse de tener MySQL corriendo localmente
# Cambiar en application.properties:
spring.datasource.url=jdbc:mysql://localhost:3306/horarios
spring.datasource.username=root
spring.datasource.password=

# Ejecutar la aplicación
./mvnw spring-boot:run
```

## 🧪 Pruebas

### Pruebas Unitarias
```bash
# Ejecutar todas las pruebas
./mvnw test

# Ejecutar pruebas específicas
./mvnw test -Dtest=NotificationServiceTest
```

### Testing de Notificaciones
```bash
# Testing individual (1 notificación)
POST /api/notifications/test/schedule-notification?testEmail=tu-email@gmail.com

# Testing completo (16 notificaciones de todos los tipos)
POST /api/notifications/test/all-notifications?testEmail=tu-email@gmail.com
```

**Nota:** Los endpoints de testing requieren autenticación con rol COORDINADOR.

## 📚 Documentación

- [Documentación Completa de Notificaciones](docs/NOTIFICACIONES_SGH_COMPLETO.md)
- [API Endpoints](http://localhost:8082/swagger-ui/index.html)

## 🐛 Solución de Problemas

### Error de conexión a MySQL
```bash
# Verificar estado de contenedores
docker-compose ps

# Ver logs de MySQL
docker-compose logs mysql

# Ver logs de la aplicación
docker-compose logs app

# Reiniciar servicios
docker-compose down
docker-compose up --build
```

### Error de puerto ocupado
```bash
# Cambiar puerto en application.properties
server.port=8083

# O cambiar en docker-compose.yml
ports:
  - "8083:8082"
```

## 📞 Soporte

Para problemas con el sistema de notificaciones, revisar:
1. Configuración de email en `application.properties`
2. Conexión a base de datos
3. Logs de la aplicación
4. Documentación en `docs/NOTIFICACIONES_SGH_COMPLETO.md`