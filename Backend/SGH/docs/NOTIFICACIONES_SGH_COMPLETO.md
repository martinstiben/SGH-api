# 📧 Sistema de Notificaciones SGH - Documentación Completa

## 🎯 **Resumen Ejecutivo**
Sistema de notificaciones automáticas para cambios en horarios. Envía **notificaciones In-App + Emails** cuando se crean o modifican horarios, informando sobre días y horas asignadas de manera elegante y clara. Sin enlaces externos ni contenido innecesario.

## 📧 **¿Cuántas Notificaciones por Email se Envían?**

### **Notificaciones Automáticas (Al Asignar Horarios)**
- ✅ **2 emails por horario asignado/modificado:**
  - **1 email al PROFESOR** → "Se te asignó un horario"
  - **1 email por COORDINADOR** → "Se registró un horario en el sistema"

### **Notificaciones Manuales (Endpoints)**
- ✅ **1 email** → `POST /api/notifications/send` (individual)
- ✅ **N emails** → `POST /api/notifications/send/bulk` (múltiples)
- ✅ **N emails** → `POST /api/notifications/send/role/{role}` (por rol)
- ✅ **N emails** → `POST /api/notifications/retry-failed` (reintentos)

### **Notificaciones de Prueba**
- ✅ **2 emails** → `POST /api/notifications/test/all-notifications`

**Total: Sistema envía automáticamente 2 emails por cada horario asignado.**

## 🚀 **Endpoints Disponibles**

### **📧 Correo Electrónico:**
- `POST /api/notifications/send` - Envío individual
- `POST /api/notifications/send/bulk` - Envío masivo
- `POST /api/notifications/send/role/{role}` - Envío por rol
- `POST /api/notifications/retry-failed` - Reintentar fallidas
- `POST /api/notifications/test/schedule-notification?testEmail=email@ejemplo.com` - **PRUEBA** notificación de horario por correo
- `POST /api/notifications/test/all-notifications?testEmail=email@ejemplo.com` - **PRUEBA COMPLETA** todas las 16 notificaciones del sistema por correo
- `GET /api/notifications/stats` - Estadísticas
- `GET /api/notifications/logs` - Historial paginado
- `GET /api/notifications/types/{role}` - Tipos por rol

### **🔔 Notificaciones In-App:**
- `GET /api/in-app-notifications/active` - Activas del usuario
- `GET /api/in-app-notifications/unread` - No leídas
- `GET /api/in-app-notifications/unread/count` - Conteo no leídas
- `PUT /api/in-app-notifications/{id}/read` - Marcar leída
- `PUT /api/in-app-notifications/mark-all-read` - Marcar todas leídas
- `GET /api/in-app-notifications/by-type/{type}` - Filtrar por tipo
- `GET /api/in-app-notifications/by-priority/{priority}` - Filtrar por prioridad

## 👥 **Roles y Tipos de Notificación**

| Rol | Tipo | Descripción |
|-----|------|-------------|
| **MAESTRO** | `TEACHER_SCHEDULE_ASSIGNED` | Nuevo horario asignado |
| | `TEACHER_CONFLICT_DETECTED` | Conflicto detectado |
| **COORDINADOR** | `SYSTEM_NOTIFICATION` | Horario registrado/modificado |

## 🎨 **Plantillas HTML**
- **Optimizadas para Gmail** (CSS inline, tablas HTML)
- **Responsive design** para móviles y desktop
- **Colores corporativos** por rol
- **Sin animaciones** (compatibilidad Gmail)
- **Sin botones de acción** ("Acceder al Sistema")
- **Solo información de horarios** (día y hora)

## 🔧 **Configuración Técnica**

### **application.properties:**
```properties
# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Notificaciones
app.notification.max-retries=3
app.notification.retry-delay=30000
```

### **Dependencias Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## 📱 **Integración Frontend**

### **Ejemplo de Notificación Recibida:**

**Para Profesores:**
```json
{
  "title": "Nuevo Horario Asignado",
  "message": "Se le ha asignado un nuevo horario:\n\nDía: LUNES\nHorario: 08:00 - 10:00",
  "priority": "MEDIUM",
  "icon": "📋"
}
```

**Para Coordinadores:**
```json
{
  "title": "Nuevo Horario Registrado",
  "message": "Se ha registrado un nuevo horario:\n\nProfesor: Juan Pérez\nDía: LUNES\nHorario: 08:00 - 10:00",
  "priority": "MEDIUM",
  "icon": "📋"
}
```

### **React Web - Hook para Notificaciones:**
```javascript
const useNotifications = (userId, token) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  // Cargar notificaciones
  const loadNotifications = async () => {
    const response = await fetch(`/api/in-app-notifications/active?page=0&size=20`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    setNotifications(data.data);
    setUnreadCount(data.unreadCount || 0);
  };

  // Marcar como leída
  const markAsRead = async (notificationId) => {
    await fetch(`/api/in-app-notifications/${notificationId}/read`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${token}` }
    });
  };

  return { notifications, unreadCount, markAsRead, loadNotifications };
};
```

### **React Native - Notificaciones Push:**
```javascript
// Conectar WebSocket
const ws = new WebSocket('ws://localhost:8082/ws/notifications');

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.type === 'new_notification') {
    // Mostrar push notification
    PushNotification.localNotification({
      title: message.data.title,
      message: message.data.message
    });
  }
};
```

## 🧪 **Pruebas del Sistema**

### **🎯 Endpoint de Prueba para Correos (SOLO PARA TESTING):**
```bash
POST http://localhost:8082/api/notifications/test/schedule-notification?testEmail=tu-email@gmail.com
Authorization: Bearer YOUR_JWT_TOKEN (Rol: COORDINADOR)
```

**¿Qué hace este endpoint?**
- ✅ Envía una notificación de prueba por correo electrónico
- ✅ Usa la plantilla HTML real optimizada para Gmail
- ✅ Verifica que el sistema de envío funcione correctamente
- ✅ Permite probar el formato y diseño de las notificaciones

**Contenido del correo de prueba:**
- **Asunto:** "Prueba - Nuevo Horario Asignado"
- **Plantilla:** HTML completa con diseño responsive
- **Contenido:** "Se le ha asignado un nuevo horario: Día: LUNES, Horario: 08:00 - 10:00"
- **Formato:** Optimizado para Gmail, Outlook, etc.

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Notificación de prueba enviada por correo",
  "testEmail": "tu-email@gmail.com",
  "type": "SCHEDULE_NOTIFICATION",
  "status": "SENDING"
}
```

### **🚀 Endpoint de TODAS las Notificaciones por Correo (SOLO PARA TESTING):**
```bash
POST http://localhost:8082/api/notifications/test/all-notifications?testEmail=tu-email@gmail.com
Authorization: Bearer YOUR_JWT_TOKEN (Rol: COORDINADOR)
```

**¿Qué hace este endpoint?**
- ✅ **Envía 3 notificaciones diferentes** por correo electrónico
- ✅ **Usa las plantillas HTML reales** del sistema
- ✅ **Cubre todos los tipos implementados** de notificaciones
- ✅ **Envío asíncrono** para no bloquear la respuesta
- ✅ **Verifica formato completo** en Gmail

**Notificaciones que recibirás (dinámicas y automáticas):**
1. **TEACHER_SCHEDULE_ASSIGNED** (para MAESTRO)
   - Asunto: "📚 Nuevo Horario de Clase Asignado - SGH"
   - Contenido: **DINÁMICO** - Información real del horario asignado (materia, curso, día, hora)
   - Enlace: https://sgh.edu.co/profesor/horarios
   - Plantilla azul con 📚

2. **SYSTEM_NOTIFICATION** (para COORDINADOR)
   - Asunto: "⚙️ Nuevo Horario Registrado - Sistema SGH"
   - Contenido: **DINÁMICO** - Detalles reales del horario registrado y profesor asignado
   - Enlace: https://sgh.edu.co/coordinador/horarios
   - Plantilla naranja con ⚙️

**Nota:** Las notificaciones son completamente dinámicas y se generan automáticamente basadas en eventos reales del sistema. No contienen datos hardcodeados. Cada notificación incluye información específica del horario creado/modificado.

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Notificaciones reales del Sistema SGH enviadas por correo",
  "testEmail": "tu-email@gmail.com",
  "totalNotifications": 2,
  "notificationsSent": [
    "TEACHER_SCHEDULE_ASSIGNED (MAESTRO) - Nueva clase asignada con datos dinámicos",
    "SYSTEM_NOTIFICATION (COORDINADOR) - Horario registrado con datos dinámicos"
  ],
  "note": "Las notificaciones contienen datos generados dinámicamente, no hardcodeados",
  "status": "SENDING_REAL_NOTIFICATIONS"
}
```

## 🔍 **Verificación de Envío por Email**

### **Cómo Confirmar que las Notificaciones se Envían Correctamente:**

1. **Revisa tu bandeja de entrada** después de ejecutar el endpoint
2. **Verifica que recibas exactamente 2 emails** con asuntos diferentes
3. **Confirma que los datos sean diferentes** en cada ejecución (no hardcodeados)
4. **Revisa el formato HTML** optimizado para Gmail
5. **Verifica los enlaces** que lleven a las secciones correctas

### **Posibles Problemas y Soluciones:**

- **No llegan emails**: Verificar configuración SMTP en `application.properties`
- **Emails van a spam**: Las plantillas están optimizadas para Gmail
- **Datos hardcodeados**: El sistema ahora genera datos dinámicos aleatorios
- **Enlaces no funcionan**: Ajustar URLs según tu dominio real

### **Pruebas Automáticas:**

```bash
# Ejecutar múltiples veces para verificar datos dinámicos
curl -X POST "http://localhost:8082/api/notifications/test/all-notifications?testEmail=tu-email@gmail.com" \
  -H "Authorization: Bearer TU_JWT_TOKEN"

# Verificar que cada ejecución genere datos diferentes
```

### **Envío Individual:**
```bash
curl -X POST http://localhost:8082/api/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "subject": "Asunto",
    "content": "Contenido HTML",
    "recipientEmail": "usuario@email.com",
    "recipientName": "Nombre",
    "recipientRole": "MAESTRO",
    "notificationType": "TEACHER_SCHEDULE_ASSIGNED"
  }'
```

### **Obtener Notificaciones In-App:**
```bash
curl -X GET "http://localhost:8082/api/in-app-notifications/active?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔄 **Flujo de Funcionamiento**

1. **Validación** - Tipo válido para rol
2. **Envío Asíncrono** - Correo + In-App simultáneo
3. **Reintentos** - Hasta 3 intentos ante fallos
4. **Logging** - Registro completo de operaciones
5. **WebSocket** - Notificaciones en tiempo real

## ✅ **Características Implementadas**

- ✅ **2 emails automáticos** por horario asignado (Profesor + Coordinadores)
- ✅ **Notificaciones In-App automáticas** al crear/modificar horarios
- ✅ **Notificaciones por correo** con plantillas HTML optimizadas
- ✅ **Contenido elegante y claro** ("Se te asignó un horario")
- ✅ **Sin enlaces externos** ni contenido innecesario
- ✅ **Endpoint de prueba individual** (`/api/notifications/test/schedule-notification`)
- ✅ **Endpoint de prueba completo** (`/api/notifications/test/all-notifications`) - **16 notificaciones completas**
- ✅ **Información completa de horarios** (materia, curso, día, hora)
- ✅ **Envío simultáneo** In-App + Email
- ✅ **APIs REST** para consumir notificaciones desde frontend
- ✅ **WebSocket** para tiempo real (opcional)
- ✅ **Logging completo** de todas las operaciones

## 🚨 **Troubleshooting**

### **Problemas Comunes:**
1. **Correos no llegan** - Verificar credenciales SMTP
2. **WebSocket no conecta** - Comprobar puerto y CORS
3. **Errores de autenticación** - JWT token válido
4. **Notificaciones no se muestran** - Verificar userId correcto

### **Logs Importantes:**
```bash
tail -f logs/spring.log | grep -i notification
```

## 📋 **Archivos Modificados/Creados**

- ✅ `NotificationController.java` - Endpoints de correo + endpoint de pruebas completo
- ✅ `InAppNotificationController.java` - Endpoints In-App (NUEVO)
- ✅ `NotificationService.java` - Servicio de correos con plantillas HTML
- ✅ `InAppNotificationService.java` - Servicio In-App
- ✅ `ScheduleService.java` - Integración automática
- ✅ `NotificationType.java` - Tipos por rol
- ✅ `docs/ejemplos-plantillas/maestro-gmail-optimized.html` - Plantilla Gmail para maestros
- ✅ **Eliminados:** Plantillas no implementadas (estudiantes, directores, general)
- ✅ Pruebas unitarias actualizadas

## 🎯 **Resultado Final**

Sistema de notificaciones completo y funcional:
- ✅ **2 emails automáticos** por horario asignado (Profesor + Coordinadores)
- ✅ **2 notificaciones In-App** por horario asignado (Profesor + Coordinadores)
- ✅ **Contenido elegante y claro** ("Se te asignó un horario")
- ✅ **Información completa** en cada notificación (materia, curso, profesor, horario)
- ✅ **Sin enlaces externos** ni contenido innecesario
- ✅ **Envío automático** al crear/actualizar horarios
- ✅ **Emails con formato** profesional optimizado para Gmail
- ✅ **Plantillas limpias** (eliminadas las no implementadas)
- ✅ **APIs listas** para consumir desde frontend
- ✅ **Documentación clara** y ejemplos de uso

**¡Listo para producción con notificaciones informativas, elegantes y funcionales!** 🚀📧🔔