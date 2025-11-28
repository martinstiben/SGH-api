# Sistema de Generación Automática de Horarios - SGH

## ✅ Funcionalidad Implementada y Operativa

El sistema SGH cuenta con **generación automática de horarios completamente funcional** que permite a los coordinadores generar horarios académicos con un solo clic, cumpliendo con los requisitos de la Historia de Usuario.

## 🚀 Endpoints Principales

### 1. Generación Automática por Curso (HU Principal)
```
POST /schedules/generate-course/{courseId}
```
**Función**: Genera un horario completo para un curso específico seleccionado.
**Ideal para**: Botón en interfaz para generar horario individual.

### 2. Generación Automática General
```
POST /schedules/auto-generate
```
**Función**: Genera horarios automáticamente para todos los cursos sin horario.
**Parámetros**: Automáticos (semana actual, lunes a viernes).

### 3. Diagnóstico del Sistema
```
GET /schedules/diagnostic
```
**Función**: Proporciona análisis completo del estado del sistema antes de generar horarios.

### 4. Asignación Automática de Profesores
```
POST /schedules/auto-assign
```
**Función**: Asigna profesores a cursos que no tienen profesor asignado.

## 📋 Flujo de Trabajo Recomendado

### Paso 1: Diagnóstico
```bash
curl -X GET "http://localhost:8080/schedules/diagnostic" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Paso 2: Asignar Profesores (Si es necesario)
```bash
curl -X POST "http://localhost:8080/schedules/auto-assign" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Paso 3: Generar Horarios
```bash
# Opción A: Por curso específico
curl -X POST "http://localhost:8080/schedules/generate-course/9" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Opción B: Generación automática general
curl -X POST "http://localhost:8080/schedules/auto-generate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📊 Respuesta Exitosa Típica

```json
{
  "id": 123,
  "status": "SUCCESS",
  "totalGenerated": 8,
  "totalCoursesWithoutAvailability": 0,
  "message": "Generación completada. 8 horarios generados exitosamente.",
  "executedBy": "coordinator@school.edu",
  "executedAt": "2025-11-28T11:30:00",
  "periodStart": "2025-11-24",
  "periodEnd": "2025-11-28",
  "dryRun": false,
  "coursesWithoutAvailability": []
}
```

## 🔧 Requisitos del Sistema

1. **Profesores con disponibilidad definida** (por día de la semana)
2. **Cursos con profesor asignado** (relación TeacherSubject)
3. **Usuarios con rol COORDINADOR** para generar horarios

## 🔍 Resolución de Problemas Comunes

### NO_AVAILABILITY_DEFINED
**Solución**: Registrar disponibilidad del profesor
```bash
curl -X POST "http://localhost:8080/availability/register" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"teacherId": 5, "day": "Lunes", "amStart": "08:00", "amEnd": "12:00"}'
```

### CONFLICTS_WITH_EXISTING
**Solución**: Regenerar completamente
```bash
curl -X POST "http://localhost:8080/schedules/regenerate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🧪 Testing y Validación

El sistema incluye tests automatizados que validan:
- ✅ Generación exitosa de horarios
- ✅ Distribución correcta en días de la semana
- ✅ Respeto de disponibilidades de profesores
- ✅ Ausencia de conflictos entre horarios
- ✅ Funcionamiento del endpoint por curso específico

## ✅ Estado Final

**🎯 LA FUNCIONALIDAD ESTÁ COMPLETAMENTE IMPLEMENTADA Y FUNCIONAL**

- ✅ Generación automática por curso específico (HU principal)
- ✅ Generación automática general
- ✅ Diagnóstico completo del sistema
- ✅ Asignación automática de profesores
- ✅ Detección inteligente de problemas
- ✅ Logging detallado para debugging
- ✅ Validación robusta de datos
- ✅ Tests automatizados validados

**El sistema está listo para usar en producción.**