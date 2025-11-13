# 🎨 **Ejemplos de Plantillas HTML - Sistema SGH**

## 📋 **Descripción**

Esta carpeta contiene ejemplos visuales de todas las plantillas HTML de notificaciones del Sistema de Gestión de Horarios (SGH). Cada archivo HTML muestra exactamente cómo se verán las notificaciones cuando sean enviadas por correo electrónico.

## 📁 **Archivos Disponibles**

### **🎨 Versiones de Plantillas:**

#### **🌟 Premium (Animadas - Para Visualización Web)**
Plantillas con gradientes, animaciones y efectos visuales avanzados. **Perfectas para ver en navegador.**

| Archivo | Rol | Color Principal | Características |
|---------|-----|----------------|----------------|
| **`estudiante.html`** | 🎓 Estudiante | Verde (#4CAF50) | Gradientes, animaciones, efectos hover |
| **`maestro.html`** | 👨‍🏫 Maestro | Azul (#2196F3) | Efectos flotantes, transiciones suaves |
| **`director.html`** | 👔 Director | Púrpura (#9C27B0) | Animaciones complejas, efectos premium |
| **`coordinador.html`** | ⚙️ Coordinador | Naranja (#FF5722) | Indicadores pulsantes, gradientes dinámicos |
| **`general.html`** | 📢 General | Gris (#6c757d) | Diseño institucional, efectos sutiles |

#### **📧 Simple (Compatibles con Email)**
Plantillas optimizadas para envío por correo electrónico. **Menos efectos, más compatibilidad.**

| Archivo | Rol | Color Principal | Características |
|---------|-----|----------------|----------------|
| **`estudiante-simple.html`** | 🎓 Estudiante | Verde (#4CAF50) | Colores sólidos, layout simple |
| **`maestro-simple.html`** | 👨‍🏫 Maestro | Azul (#2196F3) | Diseño limpio, máxima compatibilidad |

## 🚀 **Cómo Ver los Ejemplos**

### **Método 1: Abrir en Navegador**
```bash
# Desde la raíz del proyecto
start docs/ejemplos-plantillas/estudiante.html
start docs/ejemplos-plantillas/maestro.html
start docs/ejemplos-plantillas/director.html
start docs/ejemplos-plantillas/coordinador.html
start docs/ejemplos-plantillas/general.html
```

### **Método 2: Desde VS Code**
1. Abrir la carpeta `docs/ejemplos-plantillas/`
2. Hacer clic derecho en cualquier archivo `.html`
3. Seleccionar "Open with Live Server" (si tienes la extensión)
4. O simplemente "Open in Default Browser"

## 🎨 **Características de Diseño**

### **✨ Elementos Comunes:**
- **Gradientes dinámicos** por rol
- **Animaciones CSS sutiles** (slideIn, float, pulse)
- **Tipografía moderna** (Segoe UI)
- **Layout responsive** para móviles
- **Iconos emoji temáticos**

### **🏗️ Estructura:**
1. **Header** - Branding institucional con gradiente
2. **Contenido** - Información clara y organizada
3. **Grid de información** - Datos estructurados
4. **Call-to-action** - Botones contextuales
5. **Footer** - Información corporativa

## 📊 **Paleta de Colores por Rol**

| Rol | Header | Bordes | Botones | Footer |
|-----|--------|--------|---------|--------|
| **Estudiante** | `#4CAF50` → `#45a049` | `#4CAF50` | `#28a745` | `#4CAF50` |
| **Maestro** | `#2196F3` → `#1976D2` | `#2196F3` | `#1976D2` | `#2196F3` |
| **Director** | `#9C27B0` → `#7B1FA2` | `#9C27B0` | `#7B1FA2` | `#9C27B0` |
| **Coordinador** | `#FF5722` → `#E64A19` | `#FF5722` | `#E64A19` | `#FF5722` |
| **General** | `#6c757d` → `#495057` | `#6c757d` | `#6c757d` | `#6c757d` |

## 📧 **Tipos de Notificación por Rol**

### **🎓 Estudiante:**
- `STUDENT_SCHEDULE_ASSIGNMENT` - Asignación de horario
- `STUDENT_SCHEDULE_CHANGE` - Cambio de horario
- `STUDENT_CLASS_CANCELLATION` - Cancelación de clase

### **👨‍🏫 Maestro:**
- `TEACHER_CLASS_SCHEDULED` - Clase programada
- `TEACHER_CLASS_MODIFIED` - Clase modificada
- `TEACHER_CLASS_CANCELLED` - Clase cancelada
- `TEACHER_AVAILABILITY_CHANGED` - Cambio de disponibilidad

### **👔 Director:**
- `DIRECTOR_SCHEDULE_CONFLICT` - Conflicto de horarios
- `DIRECTOR_AVAILABILITY_ISSUE` - Problema de disponibilidad
- `DIRECTOR_SYSTEM_INCIDENT` - Incidencia del sistema

### **⚙️ Coordinador:**
- `COORDINATOR_GLOBAL_UPDATE` - Actualización global
- `COORDINATOR_SYSTEM_ALERT` - Alerta del sistema
- `COORDINATOR_CHANGE_CONFIRMATION` - Confirmación de cambio
- `COORDINATOR_MAINTENANCE_ALERT` - Mantenimiento programado

### **📢 General:**
- `GENERAL_SYSTEM_NOTIFICATION` - Notificación general

## 🔧 **Personalización**

### **Modificar Colores:**
```css
/* En el header */
background: linear-gradient(135deg, #TU_COLOR 0%, #TU_COLOR_SECUNDARIO 100%);

/* En los bordes */
border-left: 5px solid #TU_COLOR;

/* En los botones */
background: #TU_COLOR;
```

### **Cambiar Iconos:**
```css
/* En el header */
.logo::before {
    content: '🎓'; /* Cambiar emoji */
}

/* En el título */
.notification-title::before {
    content: '📚'; /* Cambiar emoji */
}
```

## 📱 **Responsive Design**

Todas las plantillas incluyen media queries para:
- **Móviles:** `max-width: 600px`
- **Tablets:** `max-width: 768px`
- **Desktop:** `min-width: 769px`

### **Breakpoints:**
```css
@media (max-width: 600px) {
    .container { margin: 10px; }
    .info-grid { grid-template-columns: 1fr; }
    .header { padding: 30px 20px; }
    .content { padding: 30px 20px; }
}
```

## 🎯 **Recomendaciones de Producción**

### **✅ Checklist Visual:**
- [x] **Gradientes consistentes** por rol
- [x] **Animaciones sutiles** (no distractivas)
- [x] **Contraste adecuado** para accesibilidad
- [x] **Tipografía legible** en todos los tamaños
- [x] **Botones CTA claros** y contextuales
- [x] **Información jerárquica** (títulos, subtítulos, datos)
- [x] **Branding consistente** (logo, colores, footer)

### **📧 Compatibilidad de Email:**
- [x] **CSS inline** para compatibilidad
- [x] **Tablas HTML** como respaldo
- [x] **Imágenes con fallback** de texto
- [x] **Enlaces absolutos** para botones
- [x] **Alt text** en imágenes

## 🚀 **Próximos Pasos**

1. **Revisar ejemplos** en navegador
2. **Ajustar colores** si es necesario
3. **Modificar contenido** según requerimientos
4. **Probar envío real** de correos
5. **Implementar en producción**

---

## 📞 **Soporte**

¿Necesitas modificar alguna plantilla o tienes preguntas sobre el diseño?

**Contacta al equipo de desarrollo** o revisa la documentación completa en `docs/NOTIFICACIONES_SGH.md`

---

**¡Las plantillas están listas para impresionar a tu comunidad educativa!** 🎓✨