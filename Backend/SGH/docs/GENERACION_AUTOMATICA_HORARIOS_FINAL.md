# Generación Automática de Horarios - Sistema SGH

## Resumen

La funcionalidad de **generación automática de horarios** del sistema SGH ha sido **completamente implementada y mejorada** con logging detallado y diagnóstico avanzado. El sistema ahora puede generar horarios automáticamente desde la interfaz, cumpliendo con la Historia de Usuario solicitada.

## ✅ Funcionalidades Implementadas

### 1. Generación Automática Básica
- **Endpoint**: `POST /schedules/auto-generate`
- **Descripción**: Genera horarios automáticamente usando parámetros por defecto (semana actual, lunes a viernes)
- **Ideal para**: Botón simple en la interfaz de usuario
- **Permisos**: Solo coordinadores

### 2. Generación Personalizada
- **Endpoint**: `POST /schedules/generate`
- **Descripción**: Permite personalizar fechas de inicio y fin, modo simulación, etc.
- **Ideal para**: Generaciones específicas por período
- **Permisos**: Solo coordinadores

### 3. Regeneración Completa
- **Endpoint**: `POST /schedules/regenerate`
- **Descripción**: Elimina todos los horarios existentes y genera nuevos automáticamente
- **Ideal para**: Reiniciar completamente la planificación
- **Permisos**: Solo coordinadores

### 4. Diagnóstico Avanzado
- **Endpoint**: `GET /schedules/diagnostic`
- **Descripción**: Proporciona diagnóstico completo del sistema antes de generar horarios
- **Ideal para**: Identificar problemas antes de intentar generar horarios
- **Permisos**: Solo coordinadores

### 5. Historial de Generaciones
- **Endpoint**: `GET /schedules/history`
- **Descripción**: Consulta el historial de todas las generaciones realizadas
- **Permisos**: Solo coordinadores

## 🔧 Mejoras Implementadas

### Logging Detallado
- El sistema ahora incluye logging extensivo que permite rastrear cada paso del proceso de generación
- Información detallada sobre:
  - Cursos procesados
  - Disponibilidades de profesores
  - Slots de tiempo generados
  - Conflictos detectados
  - Errores encontrados

### Detección Avanzada de Problemas
- **Cursos sin profesor asignado**
- **Profesores sin disponibilidad definida**
- **Conflictos con horarios existentes**
- **Slots de tiempo no disponibles**

### Análisis de Disponibilidades
- Verificación automática de disponibilidades de profesores por día
- Generación inteligente de slots de tiempo
- Filtrado de conflictos en tiempo real

## 📋 Cómo Usar el Sistema

### Paso 1: Diagnóstico (Recomendado)
Antes de generar horarios, ejecuta el diagnóstico para identificar posibles problemas:

```http
GET /schedules/diagnostic
```

**Respuesta esperada:**
- Lista de cursos y su estado
- Lista de profesores y sus disponibilidades
- Estadísticas del sistema
- Cursos potencialmente problemáticos

### Paso 2: Generación Automática
Para generar horarios automáticamente con parámetros por defecto:

```http
POST /schedules/auto-generate
```

**Parámetros automáticos:**
- Período: Semana actual (lunes a viernes)
- Modo: Generación real (no simulación)
- Parámetros: "Generación automática desde interfaz"

### Paso 3: Verificar Resultados
La respuesta incluye:
- `totalGenerated`: Número de horarios creados
- `totalCoursesWithoutAvailability`: Cursos que no pudieron ser asignados
- `coursesWithoutAvailability`: Lista detallada de cursos problemáticos
- `message`: Mensaje descriptivo del resultado

## 🏗️ Requisitos del Sistema

### Para que el sistema funcione correctamente:

1. **Cursos deben tener profesor asignado**
   - Cada curso debe tener una relación `TeacherSubject` (profesor + materia)

2. **Profesores deben tener disponibilidad definida**
   - Disponibilidad por día de la semana
   - Horarios de mañana y/o tarde

3. **Un profesor por materia**
   - Cada profesor debe estar asociado a máximo 1 materia
   - Validación automática del sistema

4. **Horarios sin conflictos**
   - El sistema evita conflictos de horarios para el mismo profesor

## 🔍 Resolución de Problemas Comunes

### Problema: "Cursos sin disponibilidad"
**Causa**: Profesores sin disponibilidad definida
**Solución**: 
1. Usar `/availability/register` para definir disponibilidades de profesores
2. Verificar que las disponibilidades sean válidas

### Problema: "Conflictos con horarios existentes"
**Causa**: Horarios ya existentes que impiden nuevas asignaciones
**Solución**: 
1. Usar `/schedules/regenerate` para limpiar y regenerar todo
2. O eliminar horarios conflictivos manualmente

### Problema: "Sin profesor asignado"
**Causa**: Curso sin relación TeacherSubject
**Solución**: 
1. Crear relación entre curso, profesor y materia
2. Verificar en `/courses` que los cursos tengan profesor asignado

### Problema: "Profesores con múltiples materias"
**Causa**: Restricción del sistema (un profesor = una materia)
**Solución**: 
1. Redistribuir profesores o materias
2. Cada profesor debe enseñar una sola materia

## 📊 Endpoint de Diagnóstico Detallado

El nuevo endpoint `/schedules/diagnostic` proporciona:

### CourseDiagnosticDTO
- `id`: ID del curso
- `courseName`: Nombre del curso
- `teacherId`: ID del profesor asignado
- `teacherName`: Nombre del profesor
- `subjectId`: ID de la materia
- `subjectName`: Nombre de la materia
- `hasScheduleAssigned`: Si ya tiene horarios
- `status`: Estado del curso (CON_HORARIO, SIN_HORARIO, SIN_PROFESOR)

### TeacherDiagnosticDTO
- `id`: ID del profesor
- `teacherName`: Nombre del profesor
- `subjectCount`: Número de materias que enseña
- `availabilityCount`: Número de disponibilidades definidas
- `canTeachMultipleSubjects`: Si enseña múltiples materias (problema)

### SystemStatistics
- `totalCourses`: Total de cursos
- `coursesWithoutSchedule`: Cursos sin horario
- `totalTeachers`: Total de profesores
- `teachersWithoutAvailability`: Profesores sin disponibilidad
- `totalExistingSchedules`: Horarios existentes
- `totalTeacherSubjects`: Relaciones profesor-materia

## 🚀 Próximos Pasos

1. **Ejecutar diagnóstico** para identificar problemas actuales
2. **Corregir problemas** encontrados en el diagnóstico
3. **Ejecutar generación automática**
4. **Verificar resultados** y cursos sin disponibilidad
5. **Ajustar disponibilidades** si es necesario
6. **Regenerar** si es requerido

## 📞 Soporte

Si el sistema no funciona como esperado:

1. Revisa los logs de la aplicación para detalles específicos
2. Usa el endpoint de diagnóstico para identificar problemas
3. Verifica que todos los requisitos estén cumplidos
4. Considera usar `/schedules/regenerate` para reiniciar completamente

## 🎯 Resultado Final

✅ **La Historia de Usuario está completamente implementada**: "Como usuario del sistema, quiero que exista un botón que genere automáticamente el horario desde la interfaz, para evitar la asignación manual y ahorrar tiempo en la planificación académica."

El sistema ahora incluye:
- Generación automática con un solo clic
- Detección inteligente de problemas
- Logging detallado para debugging
- Diagnóstico completo del estado del sistema
- Validación robusta de datos
- Manejo de errores mejorado

**El sistema está listo para usar en producción.**