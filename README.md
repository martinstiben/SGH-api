# 🧠 SGH (Sistema de Gestión de Horarios)

**SGH** es una solución integral diseñada para facilitar la **planificación, organización y control de los horarios escolares**.  
El sistema permite a los coordinadores gestionar horarios de clases, docentes y salones de forma eficiente, integrando módulos web, móvil y una API backend, junto con documentación y base de datos estructurada.

---

## 📦 Repositorios del Proyecto

### 🧩 [Backend](https://github.com/martinstiben/SGH-Backend-api.git)
API desarrollada para gestionar la lógica central del sistema **SGH**.  
Se encarga de la comunicación con la base de datos y provee los servicios necesarios para los módulos web y móvil.

---

### 🌐 [Frontend Web](https://github.com/martinstiben/SGH-Web-portal.git)
Portal web diseñado para **administradores y coordinadores escolares**, con el fin de gestionar de manera eficiente los horarios, docentes y aulas.

---

### 📱 [Frontend Móvil](https://github.com/martinstiben/SGH-Movil-app.git)
Aplicación móvil que permite **consultar y visualizar horarios** desde cualquier lugar, conectándose directamente con la API backend.

---

### 📚 [Documentación](https://github.com/martinstiben/SGH-Documentacion-docs.git)
Contiene toda la **documentación técnica y funcional** del proyecto, incluyendo manuales, diagramas UML y guías de instalación.

---

### 🗃️ [Base de Datos](https://github.com/martinstiben/SGH-BaseDeDatos-db.git)
Incluye la **estructura, scripts y procedimientos almacenados** necesarios para el funcionamiento del sistema SGH.

---

## 🧱 Arquitectura General

El sistema se compone de cinco módulos principales que trabajan de forma integrada:

```
┌───────────────────────────┐
│       SGH Frontend Web    │
└────────────┬──────────────┘
             │
┌────────────▼──────────────┐
│      SGH Backend API      │
└────────────┬──────────────┘
             │
┌────────────▼──────────────┐
│     SGH Base de Datos     │
└────────────┬──────────────┘
             │
┌────────────▼──────────────┐
│     SGH Móvil App         │
└────────────┬──────────────┘
             │
┌────────────▼──────────────┐
│     SGH Documentación     │
└───────────────────────────┘
```

---

## 📄 Licencia
Proyecto desarrollado con fines académicos y de gestión escolar.
