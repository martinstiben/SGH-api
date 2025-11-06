# Configuración de Puertos por Ambiente - SGH API

## 🌐 Puertos Asignados por Ambiente

### **Desarrollo (Develop)**
- **Puerto API**: 8082
- **Swagger UI**: http://localhost:8082/swagger-ui/index.html
- **API Docs**: http://localhost:8082/api-docs
- **Base de Datos**: PostgreSQL en puerto 5432
- **Container DB**: DB_Develop
- **URL Base**: http://localhost:8082

### **Quality Assurance (QA)**
- **Puerto API**: 8083
- **Swagger UI**: http://localhost:8083/swagger-ui/index.html
- **API Docs**: http://localhost:8083/api-docs
- **Base de Datos**: PostgreSQL en puerto 5433
- **Container DB**: DB_QA
- **URL Base**: http://localhost:8083

### **Staging**
- **Puerto API**: 8084
- **Swagger UI**: http://localhost:8084/swagger-ui/index.html
- **API Docs**: http://localhost:8084/api-docs
- **Base de Datos**: PostgreSQL en puerto 5434
- **Container DB**: DB_Staging
- **URL Base**: http://localhost:8084

### **Producción (Main)**
- **Puerto API**: 8085
- **Swagger UI**: http://localhost:8085/swagger-ui/index.html
- **API Docs**: http://localhost:8085/api-docs
- **Base de Datos**: PostgreSQL en puerto 5435
- **Container DB**: DB_Prod
- **URL Base**: http://localhost:8085

## 🚀 Acceso Rápido

Para abrir Swagger UI en el navegador después del despliegue:

### **Desarrollo:**
```bash
# Abrir automáticamente
start http://localhost:8082/swagger-ui/index.html
```

### **QA:**
```bash
# Abrir automáticamente  
start http://localhost:8083/swagger-ui/index.html
```

### **Staging:**
```bash
# Abrir automáticamente
start http://localhost:8084/swagger-ui/index.html
```

### **Producción:**
```bash
# Abrir automáticamente
start http://localhost:8085/swagger-ui/index.html
```

## 🔧 Comandos Útiles

### **Verificar que el API esté funcionando:**
```bash
curl http://localhost:8082/actuator/health
```

### **Ver todos los endpoints disponibles:**
```bash
curl http://localhost:8082/api-docs
```

### **Probar un endpoint específico:**
```bash
curl -X GET "http://localhost:8082/api/v1/courses" \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📋 Notas Importantes

- **Aislamiento**: Cada ambiente tiene su propia base de datos en puertos diferentes
- **Swagger**: Disponible en todos los ambientes para testing y documentación
- **Autenticación**: JWT token requerido para endpoints protegidos
- **CORS**: Configurado para permitir acceso desde el frontend

---

**Última actualización**: 2025-11-06
**Mantenido por**: Equipo de Desarrollo SGH