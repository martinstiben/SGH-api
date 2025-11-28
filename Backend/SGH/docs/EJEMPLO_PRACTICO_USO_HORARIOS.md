# Ejemplo Práctico: Cómo Usar la Generación Automática de Horarios

## Pasos para Generar Horarios Automáticamente

### 1. Verificar el Estado Actual del Sistema

Primero, ejecuta un diagnóstico para entender el estado actual:

```bash
curl -X GET "http://localhost:8080/schedules/diagnostic" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Ejemplo de respuesta:**
```json
{
  "courses": [
    {
      "id": 1,
      "courseName": "Matemáticas 1A",
      "teacherId": 5,
      "teacherName": "Prof. Juan Pérez",
      "subjectId": 10,
      "subjectName": "Matemáticas",
      "hasScheduleAssigned": false,
      "status": "SIN_HORARIO"
    }
  ],
  "teachers": [
    {
      "id": 5,
      "teacherName": "Prof. Juan Pérez",
      "subjectCount": 1,
      "availabilityCount": 5,
      "canTeachMultipleSubjects": false
    }
  ],
  "statistics": {
    "totalCourses": 10,
    "coursesWithoutSchedule": 8,
    "totalTeachers": 5,
    "teachersWithoutAvailability": 1,
    "totalExistingSchedules": 0,
    "totalTeacherSubjects": 10
  }
}
```

### 2. Interpretar los Resultados

**Si `coursesWithoutSchedule > 0`:** Hay cursos que necesitan horarios
**Si `teachersWithoutAvailability > 0`:** Hay profesores sin disponibilidad definida
**Si `problematicCourses` no está vacío:** Hay cursos que no se podrán asignar

### 3. Verificar Disponibilidades de Profesores

Si hay profesores sin disponibilidad, verifica sus disponibilidades:

```bash
curl -X GET "http://localhost:8080/availability/by-teacher/5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ejemplo de respuesta:**
```json
[
  {
    "id": 1,
    "teacher": {
      "id": 5,
      "teacherName": "Prof. Juan Pérez"
    },
    "day": "Lunes",
    "amStart": "08:00",
    "amEnd": "12:00",
    "pmStart": "13:00",
    "pmEnd": "17:00"
  }
]
```

### 4. Registrar Disponibilidad (Si es Necesario)

Si un profesor no tiene disponibilidad, regístrala:

```bash
curl -X POST "http://localhost:8080/availability/register" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "teacherId": 5,
    "day": "Lunes",
    "amStart": "08:00",
    "amEnd": "12:00",
    "pmStart": "13:00",
    "pmEnd": "17:00"
  }'
```

### 5. Generar Horarios Automáticamente

Ahora ejecuta la generación automática:

```bash
curl -X POST "http://localhost:8080/schedules/auto-generate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Ejemplo de respuesta exitosa:**
```json
{
  "id": 123,
  "executedBy": "coordinator@school.edu",
  "executedAt": "2025-11-27T21:30:00",
  "status": "SUCCESS",
  "totalGenerated": 8,
  "totalCoursesWithoutAvailability": 2,
  "message": "Generación completada. 8 horarios generados, 2 cursos sin disponibilidad de profesores.",
  "coursesWithoutAvailability": [
    {
      "courseId": 9,
      "courseName": "Educación Física 2B",
      "teacherId": 7,
      "teacherName": "Prof. Ana López",
      "reason": "NO_AVAILABILITY_DEFINED",
      "description": "El profesor Prof. Ana López no tiene disponibilidad configurada para ningún día: Lunes, Martes, Miércoles, Jueves, Viernes"
    }
  ],
  "periodStart": "2025-11-24",
  "periodEnd": "2025-11-28",
  "dryRun": false,
  "force": false,
  "params": "Generación automática desde interfaz"
}
```

### 6. Analizar los Resultados

**Si `status` es "SUCCESS":** La generación fue exitosa
**Si `totalGenerated > 0`:** Se crearon nuevos horarios
**Si `totalCoursesWithoutAvailability > 0`:** Algunos cursos no pudieron ser asignados

### 7. Verificar Horarios Generados

Puedes verificar los horarios generados:

```bash
# Ver todos los horarios
curl -X GET "http://localhost:8080/schedules" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Ver horarios por curso
curl -X GET "http://localhost:8080/schedules/course/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Ver horarios por profesor
curl -X GET "http://localhost:8080/schedules/teacher/5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 8. Exportar Horarios (Opcional)

Exporta los horarios en diferentes formatos:

```bash
# PDF por curso
curl -X GET "http://localhost:8080/schedules/pdf/course/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output horario_curso_1.pdf

# Excel general
curl -X GET "http://localhost:8080/schedules/excel/all" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output horario_general_completo.xlsx
```

## Resolución de Problemas Comunes

### Problema: "NO_AVAILABILITY_DEFINED"

**Síntoma**: Cursos en `coursesWithoutAvailability` con reason "NO_AVAILABILITY_DEFINED"

**Solución**:
1. Identifica el profesor problemático
2. Registra su disponibilidad para los días necesarios
3. Vuelve a ejecutar la generación

### Problema: "CONFLICTS_WITH_EXISTING"

**Síntoma**: Cursos con reason "CONFLICTS_WITH_EXISTING"

**Solución**:
1. Verifica horarios existentes del profesor
2. Elimina horarios conflictivos o usa regeneración completa

### Problema: "NO_TIME_SLOTS_AVAILABLE"

**Síntoma**: Cursos con reason "NO_TIME_SLOTS_AVAILABLE"

**Solución**:
1. Revisa las disponibilidades del profesor
2. Amplía los rangos de tiempo si es necesario
3. Redistribuye profesores si es posible

### Regeneración Completa

Si nada funciona, usa la regeneración completa:

```bash
curl -X POST "http://localhost:8080/schedules/regenerate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

## Logging y Debugging

El sistema ahora incluye logging detallado. Para ver los logs:

```bash
# En la aplicación Spring Boot
tail -f logs/application.log | grep "ScheduleGenerationService"
```

Los logs muestran:
- ✅ Progreso de la generación
- 🔍 Detalles de cada curso procesado
- ⚠️ Problemas encontrados
- 📊 Estadísticas finales

## Consejos Adicionales

1. **Siempre ejecuta el diagnóstico primero**
2. **Verifica disponibilidades antes de generar**
3. **Usa regeneración completa para empezar desde cero**
4. **Revisa los logs para debugging detallado**
5. **Considera exportar horarios después de la generación exitosa**

¡El sistema está listo para usar!