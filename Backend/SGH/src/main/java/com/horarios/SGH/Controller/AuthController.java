package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.DTO.LoginResponseDTO;
import com.horarios.SGH.DTO.RegisterRequestDTO;
import com.horarios.SGH.DTO.VerifyCodeDTO;
import com.horarios.SGH.DTO.PasswordResetRequestDTO;
import com.horarios.SGH.DTO.PasswordResetDTO;
import com.horarios.SGH.Service.AuthService;
import com.horarios.SGH.Service.TokenRevocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador REST para manejo de autenticación y autorización de usuarios.
 * Proporciona endpoints para login, registro, verificación de código 2FA,
 * gestión de perfiles y recuperación de contraseñas.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500", "http://localhost:3000", "http://localhost:3001"})
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService service;
    private final TokenRevocationService tokenRevocationService;
    private final com.horarios.SGH.Service.usersService usersService;

    public AuthController(AuthService service, TokenRevocationService tokenRevocationService, com.horarios.SGH.Service.usersService usersService) {
        this.service = service;
        this.tokenRevocationService = tokenRevocationService;
        this.usersService = usersService;
    }

    /**
     * Inicia el proceso de login verificando credenciales y enviando código 2FA.
     *
     * @param request DTO con email y contraseña del usuario
     * @return ResponseEntity con mensaje de confirmación o error
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión (Paso 1)", description = "Verifica credenciales con email y contraseña, y envía código de verificación al email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Código enviado exitosamente"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        logger.info("=== LOGIN REQUEST ===");
        logger.info("Email: {}", request.getEmail());
        logger.info("Password: {}", (request.getPassword() != null ? "[PROVIDED]" : "null"));
        try {
            String message = service.initiateLogin(request);
            logger.info("Login initiated, code sent");
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            logger.error("Login error details:", e);
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    /**
     * Verifica el código de verificación 2FA y genera token JWT si es válido.
     *
     * @param request DTO con email y código de verificación
     * @return ResponseEntity con token JWT o error de validación
     */
    @PostMapping("/verify-code")
    @Operation(summary = "Verificar código (Paso 2)", description = "Verifica el código de 2FA enviado al email y devuelve token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación exitosa",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeDTO request) {
        logger.info("=== VERIFY CODE REQUEST ===");
        logger.info("Email: {}", request.getEmail());
        logger.info("Code: {}", request.getCode());
        try {
            LoginResponseDTO resp = service.verifyCode(request.getEmail(), request.getCode());
            logger.info("Code verified successfully, token generated");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Code verification failed: {}", e.getMessage());
            logger.error("Code verification error details:", e);
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Registra un nuevo usuario en el sistema con validación de datos.
     *
     * @param request DTO con información del nuevo usuario
     * @return ResponseEntity con mensaje de confirmación o error de validación
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario con rol específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en el registro")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            logger.info("Registrando nuevo usuario: {}", request.getEmail());
            String msg = service.register(request.getName(), request.getEmail(), request.getPassword(), request.getRole(), request.getSubjectId(), request.getCourseId());
            logger.info("Usuario registrado exitosamente: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (IllegalArgumentException ex) {
            logger.warn("Error de validación en registro: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            logger.warn("Error de estado en registro: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error interno en registro:", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Cierra la sesión del usuario revocando el token JWT.
     *
     * @param authHeader Header de autorización con el token Bearer
     * @return ResponseEntity con confirmación o error
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Solicitud de logout recibida");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenRevocationService.revokeToken(token);
                logger.info("Token revocado exitosamente");
                return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
            } else {
                logger.warn("Intento de logout sin token válido");
                return ResponseEntity.badRequest().body(Map.of("error", "Token no proporcionado"));
            }
        } catch (Exception e) {
            logger.error("Error al cerrar sesión:", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error al cerrar sesión"));
        }
    }

    /**
     * Obtiene la información del perfil del usuario autenticado.
     *
     * @return ResponseEntity con información del perfil o error
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile() {
        try {
            logger.info("Obteniendo perfil de usuario autenticado");
            var user = service.getProfile();
            Map<String, Object> profile = new HashMap<>();
            profile.put("userId", user.getUserId());
            profile.put("name", user.getPerson().getFullName());
            profile.put("email", user.getPerson().getEmail());
            // profile.put("role", user.getRole().getRoleName()); // Comentado temporalmente

            // Agregar información del curso si es estudiante
            if (user.getCourse() != null) {
                profile.put("courseId", user.getCourse().getId());
                profile.put("courseName", user.getCourse().getCourseName());
            }

            logger.info("Perfil obtenido exitosamente para usuario: {}", user.getUserId());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error obteniendo perfil:", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo perfil"));
        }
    }

    /**
     * Actualiza la información del perfil del usuario autenticado.
     *
     * @param name Nuevo nombre del usuario (opcional)
     * @param email Nuevo email del usuario (opcional)
     * @param photo Nueva foto de perfil (opcional)
     * @return ResponseEntity con confirmación o error de validación
     */
    @PutMapping(value = "/profile", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            logger.info("Solicitud de actualización de perfil recibida");
            // Validar que al menos un campo esté presente
            if ((name == null || name.trim().isEmpty()) &&
                (email == null || email.trim().isEmpty()) &&
                (photo == null || photo.isEmpty())) {
                logger.warn("Intento de actualización de perfil sin campos válidos");
                return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar al menos un campo para actualizar"));
            }

            // Actualizar nombre si se proporcionó
            if (name != null && !name.trim().isEmpty()) {
                service.updateUserName(name);
            }

            // Actualizar email si se proporcionó
            if (email != null && !email.trim().isEmpty()) {
                service.updateUserEmail(email);
            }

            // Actualizar foto si se proporcionó
            if (photo != null && !photo.isEmpty()) {
                var user = service.getProfile();
                usersService.updateUserPhoto(user.getUserId(), photo);
            }

            logger.info("Perfil actualizado correctamente");
            return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en actualización de perfil: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Error de estado en actualización de perfil: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error actualizando perfil:", e); // Para debugging
            return ResponseEntity.status(500).body(Map.of("error", "Error actualizando perfil: " + e.getMessage()));
        }
    }


    /**
     * Obtiene la lista de roles disponibles para registro de usuarios.
     *
     * @return ResponseEntity con lista de roles o error
     */
    @GetMapping("/roles")
    @Operation(summary = "Obtener roles disponibles", description = "Devuelve la lista de roles disponibles para registro")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
    })
    public ResponseEntity<?> getRoles() {
        try {
            logger.info("Obteniendo lista de roles disponibles");
            List<Map<String, String>> roles = Arrays.stream(new String[]{"MAESTRO", "ESTUDIANTE"})
                .map(role -> Map.of(
                    "value", role,
                    "label", getRoleLabel(role)
                ))
                .collect(Collectors.toList());
            logger.info("Lista de roles obtenida exitosamente");
            return ResponseEntity.ok(Map.of("roles", roles));
        } catch (Exception e) {
            logger.error("Error obteniendo roles:", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo roles"));
        }
    }

    private String getRoleLabel(String role) {
        switch (role) {
            case "MAESTRO":
                return "Maestro";
            case "COORDINADOR":
                return "Coordinador";
            case "ESTUDIANTE":
                return "Estudiante";
            case "DIRECTOR_DE_AREA":
                return "Director de Área";
            default:
                return role;
        }
    }

    /**
     * Obtiene la lista de usuarios pendientes de aprobación por el coordinador.
     *
     * @return ResponseEntity con lista de usuarios pendientes o error
     */
    @GetMapping("/pending-users")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Obtener usuarios pendientes de aprobación", description = "Obtiene la lista de usuarios que están pendientes de aprobación por el coordinador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    public ResponseEntity<?> getPendingUsers() {
        try {
            logger.info("Obteniendo lista de usuarios pendientes de aprobación");
            var pendingUsers = service.getPendingUsers();
            logger.info("Encontrados {} usuarios pendientes", pendingUsers.size());
            var result = pendingUsers.stream()
                .map(user -> {
                    // Validar que las relaciones no sean null
                    String name = (user.getPerson() != null) ? user.getPerson().getFullName() : "N/A";
                    String email = (user.getPerson() != null) ? user.getPerson().getEmail() : "N/A";
                    // String role = (user.getRole() != null) ? user.getRole().getRoleName() : "N/A";

                    return Map.of(
                        "userId", user.getUserId(),
                        "name", name,
                        "email", email,
                        // "role", role,
                        "createdAt", user.getCreatedAt()
                    );
                })
                .toList();
            logger.info("Lista de usuarios pendientes procesada exitosamente");
            return ResponseEntity.ok(Map.of("pendingUsers", result));
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios pendientes:", e); // Para debugging
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo usuarios pendientes: " + e.getMessage()));
        }
    }

    /**
     * Aprueba un usuario pendiente de aprobación.
     *
     * @param userId ID del usuario a aprobar
     * @return ResponseEntity con confirmación o error
     */
    @PostMapping("/approve-user/{userId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Aprobar usuario", description = "Aprueba un usuario pendiente de aprobación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario aprobado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la aprobación")
    })
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        try {
            logger.info("Aprobando usuario con ID: {}", userId);
            String message = service.approveUser(userId);
            logger.info("Usuario aprobado exitosamente: {}", userId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Error en aprobación de usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error interno en aprobación de usuario {}:", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Solicita restablecimiento de contraseña enviando código de verificación.
     *
     * @param request DTO con email del usuario
     * @return ResponseEntity con confirmación o error
     */
    @PostMapping("/request-password-reset")
    @Operation(summary = "Solicitar restablecimiento de contraseña", description = "Envía un email con enlace para restablecer la contraseña")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email enviado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la solicitud")
    })
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        try {
            logger.info("Solicitud de restablecimiento de contraseña para: {}", request.getEmail());
            String message = service.requestPasswordReset(request.getEmail());
            logger.info("Código de restablecimiento enviado exitosamente a: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Error en solicitud de restablecimiento para {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error interno en solicitud de restablecimiento para {}:", request.getEmail(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Verifica el código de restablecimiento y cambia la contraseña.
     *
     * @param request DTO con email, código de verificación y nueva contraseña
     * @return ResponseEntity con confirmación o error
     */
    @PostMapping("/verify-reset-code")
    @Operation(summary = "Verificar código de reset (Paso 2)", description = "Verifica el código de reset y cambia la contraseña si es válido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public ResponseEntity<?> verifyResetCode(@Valid @RequestBody PasswordResetDTO request) {
        try {
            logger.info("Verificando código de restablecimiento para: {}", request.getEmail());
            String message = service.resetPassword(request.getEmail(), request.getVerificationCode(), request.getNewPassword());
            logger.info("Contraseña restablecida exitosamente para: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Error en verificación de código para {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error interno en verificación de código para {}:", request.getEmail(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Rechaza un usuario pendiente de aprobación con motivo especificado.
     *
     * @param userId ID del usuario a rechazar
     * @param request Map con el motivo de rechazo (clave "reason")
     * @return ResponseEntity con confirmación o error
     */
    @PostMapping("/reject-user/{userId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Rechazar usuario", description = "Rechaza un usuario pendiente de aprobación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario rechazado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en el rechazo")
    })
    public ResponseEntity<?> rejectUser(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            logger.info("Rechazando usuario {} con motivo: {}", userId, reason);
            String message = service.rejectUser(userId, reason);
            logger.info("Usuario {} rechazado exitosamente", userId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Error en rechazo de usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error interno en rechazo de usuario {}:", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }
}