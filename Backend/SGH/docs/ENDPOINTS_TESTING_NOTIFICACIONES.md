# ENDPOINTS DE TESTING - NOTIFICACIONES SGH

## 🎯 **Resumen**
Sistema de testing simplificado y enfocado en las notificaciones principales automatizadas del Sistema de Gestión de Horarios (SGH).

## 📧 **ENDPOINTS DISPONIBLES**

### 1. **Testing Individual**
**`POST /api/notifications/test/schedule-notification`**
- **Propósito**: Probar una notificación específica de horario docente
- **Tipo**: TEACHER_SCHEDULE_ASSIGNED
- **Rol**: MAESTRO

### 2. **Testing Completo**
**`POST /api/notifications/test/all-notifications`**
- **Propósito**: Enviar las notificaciones principales automatizadas
- **Total**: 4 notificaciones esenciales
- **Cobertura**: Notificaciones críticas del sistema por rol

## 📊 **NOTIFICACIONES PRINCIPALES**

### 👨‍🏫 **PROFESORES**
- ✅ **TEACHER_SCHEDULE_ASSIGNED** - Nueva asignación de clase docente
  - Se activa automáticamente cuando se asigna un horario a un profesor
  - Incluye detalles específicos: materia, curso, aula, horario
  - Formato HTML azul profesional con información docente

### 📚 **ESTUDIANTES**
- ✅ **SCHEDULE_ASSIGNED** - Horario académico asignado
  - Se activa automáticamente cuando se asigna un horario estudiantil
  - Incluye información completa del horario y profesor
  - Formato HTML verde con diseño estudiantil amigable

### 👔 **DIRECTORES**
- ✅ **SYSTEM_ALERT** - Alerta crítica del sistema
  - Se activa para conflictos críticos que requieren atención inmediata
  - Incluye detalles de conflictos, profesores afectados, acciones requeridas
  - Formato HTML rojo con indicadores de alta prioridad

### ⚙️ **COORDINADORES**
- ✅ **SYSTEM_NOTIFICATION** - Notificación administrativa del sistema
  - Se activa para actualizaciones importantes y estadísticas del sistema
  - Incluye métricas, estadísticas y detalles administrativos
  - Formato HTML naranja con información de gestión

## 🚀 **USO DEL ENDPOINT**

### **Testing Individual**
```bash
curl -X POST "http://localhost:8082/api/notifications/test/schedule-notification?testEmail=tu-email@gmail.com" \
  -H "Authorization: Bearer TU_TOKEN_JWT"
```

### **Testing Completo**
```bash
curl -X POST "http://localhost:8082/api/notifications/test/all-notifications?testEmail=tu-email@gmail.com" \
  -H "Authorization: Bearer TU_TOKEN_JWT"
```

## 📋 **RESPUESTA DEL ENDPOINT**

```json
{
  "success": true,
  "message": "Notificaciones principales del Sistema SGH enviadas por correo",
  "testEmail": "tu-email@gmail.com",
  "totalNotifications": 3,
  "notificationsByRole": {
    "MAESTRO": ["TEACHER_SCHEDULE_ASSIGNED"],
    "ESTUDIANTE": ["SCHEDULE_ASSIGNED"],
    "DIRECTOR_DE_AREA": ["SYSTEM_ALERT"],
    "COORDINADOR": ["SYSTEM_NOTIFICATION"]
  },
  "note": "Se enviaron las 4 notificaciones principales automatizadas del sistema SGH",
  "status": "SENDING_CORE_SYSTEM_NOTIFICATIONS"
}
```

## 🎨 **CARACTERÍSTICAS**

### ✅ **Contenido Automatizado**
- Datos dinámicos realistas para cada notificación
- Materias, cursos, profesores y horarios variables
- Activación automática cuando ocurren eventos del sistema

### ✅ **Plantillas Profesionales**
- HTML optimizado para Gmail
- Diseño responsive y moderno
- Colores temáticos por tipo de notificación:
  - 🔵 Azul para profesores (#2196F3)
  - 🟢 Verde para estudiantes (#4CAF50)
  - 🟠 Naranja para sistema (#FF9800)

### ✅ **Integración Completa**
- Notificaciones por email automáticas
- Notificaciones in-app para la aplicación
- Logging completo de todas las notificaciones
- Reintentos automáticos en caso de fallo

## 🔐 **AUTENTICACIÓN**
- **Rol requerido**: COORDINADOR
- **Token JWT**: Necesario en el header Authorization

## 📈 **ESTADÍSTICAS**
- **Total de tipos**: 4 notificaciones principales
- **Roles cubiertos**: 4 (Profesor, Estudiante, Director, Coordinador)
- **Automatización**: Activación automática por eventos del sistema
- **Formatos**: HTML profesional único por rol + notificaciones in-app
- **Envío**: Asíncrono con sistema de reintentos
- **Colores temáticos**: Azul, Verde, Rojo, Naranja por rol

## 🎯 **PROPÓSITO**
Este sistema de testing permite verificar que:
1. Las 4 notificaciones principales se envían correctamente por rol
2. Cada plantilla HTML tiene estilos únicos y profesionales por rol
3. Las notificaciones tienen contenido específico y coherente
4. La automatización funciona cuando ocurren eventos del sistema
5. La integración email + in-app está operativa
6. Los usuarios de cada rol reciben información relevante y útil
7. Los colores temáticos (azul, verde, rojo, naranja) se aplican correctamente