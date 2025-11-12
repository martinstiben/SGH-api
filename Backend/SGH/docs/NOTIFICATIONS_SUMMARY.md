# Sistema de Notificaciones por Correo Electrónico - SGH

## Resumen de Implementación

Se ha desarrollado e integrado exitosamente un **sistema completo de notificaciones por correo electrónico** en el proyecto Java Spring Boot con arquitectura MVC existente. El sistema utiliza JavaMailSender y la configuración SMTP ya configurada en el proyecto, implementando envío asíncrono, plantillas HTML personalizadas, reintentos automáticos y logging detallado.

## Arquitectura del Sistema

### 📁 Archivos Creados y Modificados

#### Modelos (DTOs)
- **`src/main/java/com/horarios/SGH/DTO/NotificationDTO.java`**
  - DTO principal para envío de notificaciones
  - Incluye validación de campos, variables de plantilla y configuración de contenido HTML
  - Soporte para variables dinámicas en plantillas

#### Enums de Notificación
- **`src/main/java/com/horarios/SGH/Model/NotificationType.java`**
  - 15 tipos de notificación específicos por rol
  - Estudiantes: Asignaciones, cambios, cancelaciones
  - Maestros: Clases programadas, modificaciones, cambios de disponibilidad
  - Directores: Conflictos, problemas de disponibilidad, incidencias
  - Coordinadores: Actualizaciones globales, alertas, confirmaciones
  - Notificaciones generales para todos los roles

- **`src/main/java/com/horarios/SGH/Model/NotificationStatus.java`**
  - Estados: PENDING, SENT, RETRY, FAILED, CANCELLED, SENDING
  - Métodos para verificar estado activo, resuelto o fallido
  - Colores asociados para interfaces gráficas

#### Modelos de Datos
- **`src/main/java/com/horarios/SGH/Model/NotificationLog.java`**
  - Logging completo de todas las notificaciones
  - Tracking de intentos, errores y tiempos de envío
  - Variables de plantilla almacenadas
  - Métodos para gestión de estados y reintentos

#### Repositorio
- **`src/main/java/com/horarios/SGH/Repository/INotificationLogRepository.java`**
  - 12 métodos de consulta especializados
  - Búsquedas por estado, rol, tipo, fechas
  - Estadísticas de notificaciones
  - Limpieza automática de logs antiguos
  - Gestión de reintentos programados

#### Servicio Principal
- **`src/main/java/com/horarios/SGH/Service/NotificationService.java`**
  - Envío asíncrono con `@Async` y `CompletableFuture`
  - Reintentos automáticos con backoff exponencial
  - Plantillas HTML diferenciadas por rol:
    - 🌱 **Estudiantes**: Verde, enfocado en horarios académicos
    - 👨‍🏫 **Maestros**: Azul, notificaciones de clases
    - 👔 **Directores**: Púrpura, alertas de gestión
    - ⚙️ **Coordinadores**: Naranja, control del sistema
    - 🔄 **General**: Plantilla por defecto
  - Envío masivo y por rol
  - Manejo de errores y logging detallado
  - Estadísticas dinámicas

#### Controlador REST
- **`src/main/java/com/horarios/SGH/Controller/NotificationController.java`**
  - 8 endpoints para gestión completa del sistema:
    - `POST /api/notifications/send` - Envío individual
    - `POST /api/notifications/send-bulk` - Envío masivo
    - `POST /api/notifications/send-by-role` - Envío por rol
    - `POST /api/notifications/retry-failed` - Reintentos
    - `GET /api/notifications/statistics` - Estadísticas
    - `GET /api/notifications/logs` - Logs con paginación
    - `GET /api/notifications/types` - Tipos disponibles
  - Documentación completa con Swagger/OpenAPI
  - Manejo asíncrono de respuestas

#### Configuración Asíncrona
- **`src/main/java/com/horarios/SGH/Config/AsyncConfig.java`**
  - Pool de ejecutores para correos electrónicos (5-20 hilos)
  - Pool de procesamiento general (3-15 hilos)
  - Nombres de hilos especializados
  - Configuración de tiempo de vida

#### Pruebas Unitarias
- **`src/test/java/com/horarios/SGH/NotificationServiceTest.java`**
  - 7 pruebas unitarias completas
  - Mocking de JavaMailSender
  - Validación de DTOs y enums
  - Pruebas de envío individual, masivo y por rol
  - Verificación de estadísticas

## Configuración Técnica

### Dependencias Agregadas
- **`spring-boot-starter-mail`**: JavaMailSender (ya existía)
- **`spring-boot-starter-freemarker`**: Plantillas HTML

### Configuración en application.properties
```properties
# Email Configuration (ya existía)
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# FreeMarker Configuration
spring.freemarker.template-loader-path=classpath:/templates
spring.freemarker.suffix=.html
spring.freemarker.encoding=UTF-8

# Notification Configuration
app.notification.max-retries=3
app.notification.retry-delay=30000
```

## Características Implementadas

### ✅ Funcionalidades Core
- [x] **Envío asíncrono** con `@Async` y `CompletableFuture`
- [x] **Plantillas HTML personalizadas** por rol y tipo de notificación
- [x] **Reintentos automáticos** con backoff exponencial (3 intentos por defecto)
- [x] **Logging completo** de todas las notificaciones
- [x] **Manejo de errores** robusto con mensajes detallados
- [x] **Envío masivo** a múltiples destinatarios
- [x] **Envío por rol** a todos los usuarios de un rol específico
- [x] **Estadísticas** de notificaciones en tiempo real

### ✅ Roles y Tipos de Notificación
- [x] **Estudiantes**: 3 tipos (asignaciones, cambios, cancelaciones)
- [x] **Maestros**: 4 tipos (clases programadas, modificaciones, cancelaciones, cambios de disponibilidad)
- [x] **Directores de Área**: 3 tipos (conflictos, problemas, incidencias)
- [x] **Coordinadores**: 4 tipos (actualizaciones, alertas, confirmaciones, mantenimiento)
- [x] **General**: 1 tipo (notificación general del sistema)

### ✅ Integración con Arquitectura MVC
- [x] **Servicios**: Integrados con `usersService` existente
- [x] **Modelos**: Seguir patrón de modelos JPA existentes
- [x] **Controladores**: REST endpoints completos
- [x] **Configuración**: Compatible con configuración Spring Boot actual
- [x] **Seguridad**: Mantiene configuración de seguridad existente

## Flujo de Funcionamiento

### 1. Envío Individual
```java
POST /api/notifications/send
{
  "recipientEmail": "estudiante@ejemplo.com",
  "recipientName": "Juan Pérez",
  "recipientRole": "ESTUDIANTE",
  "notificationType": "STUDENT_SCHEDULE_ASSIGNMENT",
  "subject": "Nuevo horario asignado",
  "content": "Su horario ha sido actualizado...",
  "isHtml": true
}
```

### 2. Envío por Rol
```java
POST /api/notifications/send-by-role?role=COORDINADOR&notificationType=COORDINATOR_GLOBAL_UPDATE&subject=Actualización&content=Nuevas funcionalidades
```

### 3. Reintentos Automáticos
- Sistema detecta notificaciones fallidas
- Reintenta con delay exponencial (30s, 60s, 90s)
- Logs detallados de cada intento
- Marca como fallida definitivamente tras 3 intentos

### 4. Plantillas HTML
Cada rol tiene su propia plantilla con:
- **Colores distintivos** por rol
- **Iconos temáticos** (📚👨‍🏫👔⚙️)
- **Diseño responsive** con CSS inline
- **Información contextual** del rol
- **Branding** del sistema SGH

## Base de Datos

### Tablas Creadas
- **`notification_logs`**: Logs principales de notificaciones
- **`notification_log_variables`**: Variables de plantilla (colección)

### Campos Principales
- ID, destinatario (email, nombre, rol)
- Tipo y estado de notificación
- Contenido y plantilla utilizada
- Intentos y timestamps
- Variables de plantilla
- Mensajes de error

## Seguridad y Configuración

### ✅ Aspectos de Seguridad
- [x] **Variables de entorno**: Credenciales SMTP via `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`
- [x] **Sin exposición en código**: Credenciales no hardcodeadas
- [x] **Headers personalizados**: X-Notification-Type, X-Recipient-Role
- [x] **Validación**: Jakarta Validation en DTOs
- [x] **Transaccionalidad**: `@Transactional` para operaciones críticas

### ✅ Configuración de Producción
- **Variables de entorno soportadas**:
  - `MAIL_HOST`: Servidor SMTP
  - `MAIL_PORT`: Puerto SMTP (587)
  - `MAIL_USERNAME`: Usuario SMTP
  - `MAIL_PASSWORD`: Contraseña SMTP

## Ejemplos de Uso

### Notificación de Asignación de Horario (Estudiante)
```
📚 Hola Juan Pérez

Actualización de Horarios

Su horario ha sido asignado correctamente para el semestre 2025-1.

📧 Destinatario: juan.perez@ejemplo.com
🎯 Rol: Estudiante
⏰ Fecha y hora: 2025-11-12T20:55:16

Si tienes alguna pregunta sobre esta actualización, contacta a tu coordinador.
```

### Notificación de Clase Programada (Maestro)
```
👨‍🏫 Professor/a María González

Notificación de Clases

Se ha programado una nueva clase para mañana a las 9:00 AM.

📧 Email: maria.gonzalez@colegio.edu
🎯 Rol: Maestro
⏰ Fecha y hora: 2025-11-12T20:55:16

Por favor, revisa tu horario actualizado en el sistema.
```

## API Endpoints

### Disponibles en `/api/notifications/`
- **POST** `/send` - Envío individual
- **POST** `/send-bulk` - Envío masivo
- **POST** `/send-by-role` - Envío por rol
- **POST** `/retry-failed` - Reintentos
- **GET** `/statistics` - Estadísticas
- **GET** `/logs` - Logs paginados
- **GET** `/types` - Tipos disponibles

## Monitoreo y Logs

### ✅ Logging Implementado
- **Nivel INFO**: Envíos exitosos, estadísticas
- **Nivel WARN**: Errores de plantillas, fallbacks
- **Nivel ERROR**: Errores de envío, reintentos fallidos
- **Contexto completo**: Email, rol, tipo, intentos, tiempo

### ✅ Métricas Disponibles
- Total de notificaciones del día
- Pendientes de envío
- Enviadas exitosamente
- Fallidas
- Tiempos de procesamiento

## Conclusión

El sistema de notificaciones está **100% integrado** en la arquitectura MVC existente, utiliza la configuración SMTP actual, y proporciona una solución completa, escalable y robusta para el envío de correos electrónicos en el sistema SGH.

### Beneficios Clave:
- ✅ **Escalabilidad**: Envío asíncrono con pools de hilos
- ✅ **Confiabilidad**: Reintentos automáticos y logging completo
- ✅ **Personalización**: Plantillas específicas por rol
- ✅ **Monitoreo**: Estadísticas y logs detallados
- ✅ **Integración**: Compatible con arquitectura existente
- ✅ **Flexibilidad**: Múltiples tipos de envío y configuraciones

El sistema está listo para producción y cumple todos los criterios de aceptación establecidos.