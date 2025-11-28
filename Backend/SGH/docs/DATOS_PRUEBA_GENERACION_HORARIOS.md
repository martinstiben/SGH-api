# Datos de Prueba - Generación Automática de Horarios

## Resumen

Se han configurado datos de prueba completos en el `DataInitializer` para poder probar todas las funcionalidades de generación automática de horarios del sistema SGH.

## 📊 Datos Creados Automáticamente

### Materias (8)
- Matemáticas
- Física
- Química
- Biología
- Ética
- Historia
- Literatura
- Inglés

### Profesores (8)
Cada profesor está asignado a exactamente UNA materia y tiene disponibilidad específica:

| Profesor | Materia | Disponibilidad |
|----------|---------|---------------|
| Juan Pérez | Matemáticas | Lunes (8-12, 14-18), Miércoles (8-12) |
| María García | Física | Martes (9-13), Jueves (9-13) |
| Carlos López | Química | Viernes (10-14, 15-19) |
| Ana Rodríguez | Biología | Lunes (10-14), Jueves (10-14) |
| Pedro Martínez | Ética | Martes (11-15), Viernes (11-15) |
| Laura Sánchez | Historia | Miércoles (9-13), Viernes (9-13) |
| Miguel Torres | Literatura | Lunes (13-17), Miércoles (13-17) |
| Sofia Ramírez | Inglés | Martes (14-18), Jueves (14-18) |

### Cursos (11)
- **8 cursos con profesores asignados**: 1A, 1B, 2A, 2B, 3A, 3B, 4A, 4B
- **3 cursos sin profesores asignados**: 5A, 5B, 6A (para probar generación automática)

### Usuario Coordinador
**Nota**: El usuario coordinador debe existir en el sistema con rol COORDINADOR para poder usar los endpoints de generación automática.

## 🚀 Cómo Probar la Funcionalidad

### 1. Iniciar la Aplicación
```bash
mvn spring-boot:run
```
Los datos se crearán automáticamente al iniciar la aplicación.

### 2. Autenticación
```bash
# Login con usuario coordinador existente
POST /auth/login
{
  "userName": "{usuario_coordinador}",
  "password": "{contraseña}"
}
```

### 3. Verificar Datos Iniciales
```bash
# Ver diagnóstico del sistema
GET /schedules/diagnostic
Authorization: Bearer {token}

# Debería mostrar:
# - 8 profesores con disponibilidad
# - 8 cursos con horarios asignados
# - 3 cursos sin horarios asignados
```

### 4. Generación Automática
```bash
# Generar horarios automáticamente
POST /schedules/auto-generate
Authorization: Bearer {token}

# Respuesta esperada:
# - Total generado: 3 (los cursos sin asignar)
# - Cursos sin disponibilidad: 0 (todos deberían asignarse)
```

### 5. Verificar Resultados
```bash
# Ver todos los horarios
GET /schedules
Authorization: Bearer {token}

# Debería mostrar 11 horarios en total
```

## 🎯 Escenarios de Prueba

### Escenario 1: Generación Exitosa
- **Condición**: Sistema con datos iniciales
- **Resultado esperado**: Los 3 cursos sin asignar obtienen horarios automáticamente
- **Verificación**: `/schedules/diagnostic` muestra 0 cursos sin horario

### Escenario 2: Regeneración Completa
```bash
POST /schedules/regenerate
Authorization: Bearer {token}
```
- **Resultado**: Todos los horarios se eliminan y se vuelven a crear
- **Verificación**: Los horarios cambian pero mantienen la lógica

### Escenario 3: Simulación
```bash
POST /schedules/generate
{
  "periodStart": "2025-12-01",
  "periodEnd": "2025-12-06",
  "dryRun": true,
  "force": false,
  "params": "Simulación de prueba"
}
```
- **Resultado**: Muestra qué cursos se generarían sin crearlos realmente

## 🔍 Análisis de Disponibilidad

### Distribución por Días
- **Lunes**: Juan Pérez (Matemáticas), Ana Rodríguez (Biología), Miguel Torres (Literatura)
- **Martes**: María García (Física), Pedro Martínez (Ética), Sofia Ramírez (Inglés)
- **Miércoles**: Juan Pérez (Matemáticas), Laura Sánchez (Historia), Miguel Torres (Literatura)
- **Jueves**: María García (Física), Ana Rodríguez (Biología), Sofia Ramírez (Inglés)
- **Viernes**: Carlos López (Química), Pedro Martínez (Ética), Laura Sánchez (Historia)

### Horarios Disponibles
- **Mañana**: 8:00-12:00, 9:00-13:00, 10:00-14:00, 11:00-15:00
- **Tarde**: 13:00-17:00, 14:00-18:00, 15:00-19:00

## 📋 Endpoints para Testing

### Generación
- `POST /schedules/auto-generate` - Generación automática
- `POST /schedules/generate` - Generación personalizada
- `POST /schedules/regenerate` - Regeneración completa

### Consulta
- `GET /schedules/diagnostic` - Diagnóstico del sistema
- `GET /schedules/history` - Historial de generaciones
- `GET /schedules` - Lista de horarios

### Gestión Manual
- `GET /courses` - Ver cursos
- `GET /teachers` - Ver profesores
- `GET /teachers/availability` - Ver disponibilidades

## ✅ Verificación de Funcionamiento

Después de ejecutar la generación automática, verificar:

1. **Historial de generaciones** contiene el registro
2. **Diagnóstico** muestra 0 cursos sin horario
3. **Lista de horarios** contiene 11 registros
4. **Cada curso** tiene exactamente 1 horario asignado
5. **No hay conflictos** entre horarios del mismo profesor

## 🔧 Configuración Adicional

Si necesitas más datos de prueba, puedes:

1. **Agregar más profesores** en el `DataInitializer`
2. **Crear más cursos** sin asignación
3. **Modificar disponibilidades** para probar diferentes escenarios
4. **Agregar conflictos** intencionales para probar detección

## 📞 Solución de Problemas

### Problema: "No hay cursos sin horario asignado"
**Solución**: Los cursos ya tienen horarios. Usa `/schedules/regenerate` para limpiar y volver a generar.

### Problema: "No hay profesores con disponibilidad"
**Solución**: Verificar que el `DataInitializer` se ejecutó correctamente.

### Problema: "Cursos sin disponibilidad"
**Solución**: Revisar las disponibilidades de los profesores y ajustar si es necesario.

---

**¡El sistema está listo para pruebas completas de la funcionalidad de generación automática de horarios!**