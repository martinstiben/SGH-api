# Funcionalidad de Recuperación de Contraseña - SGH

## Descripción General

Se ha implementado la funcionalidad completa de recuperación de contraseña olvidada para el Sistema de Gestión de Horarios (SGH). Esta característica permite a los usuarios restablecer su contraseña de forma segura mediante un proceso de verificación en dos pasos, sin necesidad de intervención manual del soporte técnico.

**Compatible con Spring Boot**: Utiliza JPA/Hibernate para crear automáticamente las columnas necesarias en la base de datos. No requiere scripts SQL manuales ni configuración adicional.

## Arquitectura Implementada

### DTOs

##### PasswordResetRequestDTO
- **Ubicación**: `src/main/java/com/horarios/SGH/DTO/PasswordResetRequestDTO.java`
- **Uso**: Solicitar el reset de contraseña
- **Campos**: `email` (requerido, validado)

##### PasswordResetDTO
- **Ubicación**: `src/main/java/com/horarios/SGH/DTO/PasswordResetDTO.java`
- **Uso**: Cambiar la contraseña con código de verificación
- **Campos**:
  - `email`: Email del usuario
  - `verificationCode`: Código de 6 dígitos enviado por email
  - `newPassword`: Nueva contraseña (mínimo 8 caracteres)

### Servicios

#### AuthService
- **Ubicación**: `src/main/java/com/horarios/SGH/Service/AuthService.java`
- **Métodos nuevos**:
  - `requestPasswordReset(String email)`: Envía código de verificación por email
  - `resetPassword(String email, String code, String newPassword)`: Valida código y cambia contraseña
  - `sendPasswordResetEmail()`: Envía email con código de verificación

### Controladores

#### AuthController
- **Ubicación**: `src/main/java/com/horarios/SGH/Controller/AuthController.java`
- **Endpoints nuevos**:
  - `POST /auth/request-password-reset`: Solicitar código de verificación
  - `POST /auth/reset-password`: Validar código y cambiar contraseña

## Flujo de Funcionamiento

### Proceso de Dos Pasos (como Login 2FA)

#### Paso 1: Solicitar Código
```
Usuario ingresa email → POST /auth/request-password-reset
    ↓
Sistema valida email → Genera código 6 dígitos → Guarda en BD (passwordResetCode + passwordResetExpiration) → Envía email con distintivo 🔑
```

#### Paso 2: Verificar y Cambiar Contraseña
```
Usuario ingresa código + nueva contraseña → POST /auth/verify-reset-code
    ↓
Sistema valida código → Verifica expiración → Hashea nueva contraseña → Actualiza BD → Limpia código usado
```

## Características de Seguridad

### Validaciones Implementadas
- ✅ Email debe existir y pertenecer a cuenta activa
- ✅ Código de verificación de 6 dígitos numérico
- ✅ Expiración automática en 10 minutos
- ✅ Un solo uso por código
- ✅ Validación de fortaleza de contraseña (mínimo 8 caracteres)
- ✅ Encriptación BCrypt para nuevas contraseñas
- ✅ Código se limpia después del uso exitoso

### Protección contra Abuso
- 🔒 Solo usuarios con cuenta ACTIVE pueden solicitar reset
- 🔒 Códigos expirados no son válidos
- 🔒 Un código usado se elimina inmediatamente
- 🔒 **Campos separados**: Los códigos de reset son independientes de los códigos 2FA
- 🔒 **Sin reutilización**: Un código de login no puede usarse para reset y viceversa
- 🔒 No se permiten múltiples solicitudes simultáneas (sobrescribe código anterior)

## Plantilla de Email

La plantilla HTML está optimizada para Gmail con:
- Diseño responsive y profesional
- **Colores neutrales/grises** para mejor legibilidad
- **Emoji distintivo 🔐** en imagen blanca para diferenciarlo claramente
- Código de verificación destacado en contenedor gris
- Información de seguridad clara
- Indicadores visuales de expiración (10 minutos)
- Diseño limpio y profesional

## Endpoints API

### Paso 1: Solicitar Código de Verificación
```http
POST /auth/request-password-reset
Content-Type: application/json

{
  "email": "usuario@ejemplo.com"
}
```

**Validaciones**:
- ✅ Email requerido y formato válido (`@Email`)
- ✅ Email debe existir en BD (`findByUserName`)
- ✅ Cuenta debe estar ACTIVE (`AccountStatus.ACTIVE`)
- ✅ Email enviado con distintivo "🔑 RESET DE CONTRASEÑA"

**Respuestas**:
- `200`: `"Se ha enviado un código de verificación a su email para restablecer la contraseña"`
- `400`: `"El email es obligatorio"` / `"El email debe tener un formato válido"` / `"No se encontró una cuenta con este email"` / `"La cuenta no está activa. Contacte al administrador."`
- `500`: Error interno del servidor

### Paso 2: Verificar Código y Cambiar Contraseña
```http
POST /auth/verify-reset-code
Content-Type: application/json

{
  "email": "usuario@ejemplo.com",
  "verificationCode": "123456",
  "newPassword": "NuevaContraseña123"
}
```

**Validaciones**:
- ✅ Email requerido y formato válido (`@Email`)
- ✅ Código de verificación requerido (6 dígitos, `@NotBlank`)
- ✅ Nueva contraseña requerida (mínimo 8 caracteres, `@Size(min=8)`)
- ✅ Código debe coincidir con `passwordResetCode`
- ✅ Código no debe estar expirado (`passwordResetExpiration`)
- ✅ Cuenta debe estar ACTIVE
- ✅ Contraseña hasheada con BCrypt

**Respuestas**:
- `200`: `"Contraseña restablecida exitosamente"`
- `400`: `"El email es obligatorio"` / `"El código de verificación es obligatorio"` / `"La nueva contraseña es obligatoria"` / `"La contraseña debe tener al menos 8 caracteres"` / `"Código de verificación inválido"` / `"Código de verificación expirado"` / `"La cuenta no está activa"`
- `500`: Error interno del servidor

## Consumo desde Frontend

### Ejemplo JavaScript/React
```javascript
// Paso 1: Solicitar código de reset
const requestPasswordReset = async (email) => {
  try {
    const response = await fetch('/auth/request-password-reset', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });

    if (response.ok) {
      const data = await response.json();
      console.log(data.message); // "Se ha enviado un código..."
      // Mostrar pantalla para ingresar código y nueva contraseña
    } else {
      const error = await response.json();
      console.error(error.error);
      // Mostrar error al usuario
    }
  } catch (error) {
    console.error('Error:', error);
  }
};

// Paso 2: Verificar código y cambiar contraseña
const verifyAndResetPassword = async (email, code, newPassword) => {
  try {
    const response = await fetch('/auth/verify-reset-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        verificationCode: code,
        newPassword
      })
    });

    if (response.ok) {
      const data = await response.json();
      console.log(data.message); // "Contraseña restablecida exitosamente"
      // Redirigir a login
    } else {
      const error = await response.json();
      console.error(error.error);
      // Mostrar error al usuario
    }
  } catch (error) {
    console.error('Error:', error);
  }
};
```

### Flujo Recomendado para Frontend
1. **Pantalla 1**: Input de email + botón "Enviar código de reset"
2. **Pantalla 2**: Input de código + nueva contraseña + confirmar contraseña + botón "Restablecer contraseña"
3. **Validación Frontend**: Comparar contraseñas antes de enviar (mejor UX)
4. **Validación Backend**: Longitud mínima 8 caracteres + hasheo BCrypt
5. **Feedback**: Mensajes claros de éxito/error
6. **Timeout**: Código expira en 10 minutos

## Consideraciones Técnicas

### Base de Datos
- **Columnas agregadas automáticamente a tabla `users`**:
  - `password_reset_code`: Código temporal para reset
  - `password_reset_expiration`: Fecha de expiración del código
- **JPA/Hibernate**: Las columnas se crean automáticamente con las anotaciones `@Column`
- **Sin scripts manuales**: Spring Boot maneja la creación/actualización del esquema

### Frontend
- **Flujo sugerido**:
  1. Pantalla para ingresar email
  2. Pantalla para ingresar código + nueva contraseña
  3. Mensajes claros de éxito/error

### Configuración
- **Expiración**: 10 minutos (igual que 2FA, configurable en código)
- **Longitud código**: 6 dígitos (igual que 2FA)
- **Email**: Utiliza configuración JavaMail existente
- **Base de datos**: JPA/Hibernate crea columnas automáticamente
- **Sin configuración adicional**: Todo funciona out-of-the-box

### Logs y Auditoría
- Todas las operaciones se registran en consola
- Códigos generados y emails enviados quedan registrados
- Errores de validación se documentan completamente

## Beneficios Implementados

✅ **Simplicidad**: Reutiliza lógica existente de 2FA
✅ **Seguridad**: Verificación en dos pasos con códigos separados
✅ **Confiabilidad**: Sin entidades adicionales, menos puntos de falla
✅ **Rapidez**: Implementación más rápida y directa
✅ **Consistencia**: Sigue patrones del sistema existente
✅ **Mantenibilidad**: Código más simple y fácil de mantener
✅ **Zero-config**: JPA crea columnas automáticamente, sin scripts manuales
✅ **Frontend-friendly**: Endpoints separados como login 2FA

## Testing

Para probar la funcionalidad:

1. **Solicitar código de reset**:
    ```bash
    curl -X POST http://localhost:8080/auth/request-password-reset \
         -H "Content-Type: application/json" \
         -d '{"email":"usuario@ejemplo.com"}'
    ```

2. **Verificar email** (buscar distintivo 🔑 RESET DE CONTRASEÑA)

3. **Verificar código y cambiar contraseña**:
    ```bash
    curl -X POST http://localhost:8080/auth/verify-reset-code \
         -H "Content-Type: application/json" \
         -d '{"email":"usuario@ejemplo.com","verificationCode":"123456","newPassword":"NuevaPass123"}'
    ```

Esta implementación proporciona una solución robusta, segura y fácil de usar para la recuperación de contraseñas, integrándose perfectamente con la arquitectura existente del sistema SGH.