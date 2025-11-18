package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.DTO.LoginResponseDTO;
import com.horarios.SGH.DTO.RegisterRequestDTO;
import com.horarios.SGH.DTO.VerifyCodeDTO;
import com.horarios.SGH.Model.Role;
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

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500", "http://localhost:3000", "http://localhost:3001"})
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
public class AuthController {

    private final AuthService service;
    private final TokenRevocationService tokenRevocationService;
    private final com.horarios.SGH.Service.usersService usersService;

    public AuthController(AuthService service, TokenRevocationService tokenRevocationService, com.horarios.SGH.Service.usersService usersService) {
        this.service = service;
        this.tokenRevocationService = tokenRevocationService;
        this.usersService = usersService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión (Paso 1)", description = "Verifica credenciales con email y contraseña, y envía código de verificación al email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Código enviado exitosamente"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        System.out.println("=== LOGIN REQUEST ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Password: " + (request.getPassword() != null ? "[PROVIDED]" : "null"));
        try {
            String message = service.initiateLogin(request);
            System.out.println("Login initiated, code sent");
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @PostMapping("/verify-code")
    @Operation(summary = "Verificar código (Paso 2)", description = "Verifica el código de 2FA enviado al email y devuelve token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación exitosa",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeDTO request) {
        System.out.println("=== VERIFY CODE REQUEST ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Code: " + request.getCode());
        try {
            LoginResponseDTO resp = service.verifyCode(request.getEmail(), request.getCode());
            System.out.println("Code verified successfully, token generated");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            System.out.println("Code verification failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario con rol específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en el registro")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            String msg = service.register(request.getName(), request.getEmail(), request.getPassword(), request.getRole());
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenRevocationService.revokeToken(token);
                return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Token no proporcionado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al cerrar sesión"));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile() {
        try {
            var user = service.getProfile();
            return ResponseEntity.ok(Map.of("userId", user.getUserId(), "name", user.getPerson().getFullName(), "email", user.getPerson().getEmail(), "role", user.getRole().getRoleName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo perfil"));
        }
    }

    @PutMapping(value = "/profile", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            // Validar que al menos un campo esté presente
            if ((name == null || name.trim().isEmpty()) &&
                (email == null || email.trim().isEmpty()) &&
                (photo == null || photo.isEmpty())) {
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

            return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Para debugging
            return ResponseEntity.status(500).body(Map.of("error", "Error actualizando perfil: " + e.getMessage()));
        }
    }


    @GetMapping("/roles")
    @Operation(summary = "Obtener roles disponibles", description = "Devuelve la lista de roles disponibles para registro")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
    })
    public ResponseEntity<?> getRoles() {
        try {
            List<Map<String, String>> roles = Arrays.stream(Role.values())
                .filter(role -> role == Role.MAESTRO || role == Role.ESTUDIANTE)
                .map(role -> Map.of(
                    "value", role.name(),
                    "label", getRoleLabel(role)
                ))
                .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("roles", roles));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo roles"));
        }
    }

    private String getRoleLabel(Role role) {
        switch (role) {
            case MAESTRO:
                return "Maestro";
            case COORDINADOR:
                return "Coordinador";
            case ESTUDIANTE:
                return "Estudiante";
            case DIRECTOR_DE_AREA:
                return "Director de Área";
            default:
                return role.name();
        }
    }

    @GetMapping("/pending-users")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Obtener usuarios pendientes de aprobación", description = "Obtiene la lista de usuarios que están pendientes de aprobación por el coordinador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    public ResponseEntity<?> getPendingUsers() {
        try {
            var pendingUsers = service.getPendingUsers();
            var result = pendingUsers.stream()
                .map(user -> {
                    // Validar que las relaciones no sean null
                    String name = (user.getPerson() != null) ? user.getPerson().getFullName() : "N/A";
                    String email = (user.getPerson() != null) ? user.getPerson().getEmail() : "N/A";
                    String role = (user.getRole() != null) ? user.getRole().getRoleName() : "N/A";

                    return Map.of(
                        "userId", user.getUserId(),
                        "name", name,
                        "email", email,
                        "role", role,
                        "createdAt", user.getCreatedAt()
                    );
                })
                .toList();
            return ResponseEntity.ok(Map.of("pendingUsers", result));
        } catch (Exception e) {
            e.printStackTrace(); // Para debugging
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo usuarios pendientes: " + e.getMessage()));
        }
    }

    @PostMapping("/approve-user/{userId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Aprobar usuario", description = "Aprueba un usuario pendiente de aprobación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario aprobado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la aprobación")
    })
    public ResponseEntity<?> approveUser(@PathVariable int userId) {
        try {
            String message = service.approveUser(userId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/reject-user/{userId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Rechazar usuario", description = "Rechaza un usuario pendiente de aprobación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario rechazado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en el rechazo")
    })
    public ResponseEntity<?> rejectUser(@PathVariable int userId, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            String message = service.rejectUser(userId, reason);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }
}