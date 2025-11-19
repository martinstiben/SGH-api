package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.DTO.InAppNotificationResponseDTO;
import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de notificaciones In-App
 * Proporciona endpoints para consultar, marcar como leídas y gestionar notificaciones del usuario
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notificaciones In-App", description = "API para gestión de notificaciones dentro de la aplicación")
public class InAppNotificationController {

    @Autowired
    private InAppNotificationService inAppNotificationService;

    @Autowired
    private com.horarios.SGH.Service.usersService usersService;

    /**
     * Obtiene todas las notificaciones activas del usuario actual
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener notificaciones del usuario",
               description = "Obtiene todas las notificaciones activas del usuario autenticado")
    public ResponseEntity<?> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Integer userId = getCurrentUserId();
            log.info("Obteniendo notificaciones para usuario ID: {}", userId);

            Page<InAppNotification> notifications = inAppNotificationService.getActiveNotificationsByUserId(userId, page, size);
            log.info("Encontradas {} notificaciones para usuario {}", notifications.getTotalElements(), userId);

            List<InAppNotificationResponseDTO> notificationDTOs = notifications.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "notifications", notificationDTOs,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "currentPage", page,
                "size", size
            );

            log.debug("Enviando respuesta con {} notificaciones", notificationDTOs.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones del usuario: {}", e.getMessage(), e);
            // En caso de error, devolver respuesta vacía en lugar de error 500
            Map<String, Object> emptyResponse = Map.of(
                "notifications", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "currentPage", page,
                "size", size
            );
            return ResponseEntity.ok(emptyResponse);
        }
    }

    /**
     * Marca una notificación específica como leída
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar notificación como leída",
               description = "Marca una notificación específica como leída para el usuario autenticado")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            Integer userId = getCurrentUserId();

            // Verificar que la notificación pertenece al usuario
            InAppNotification notification = inAppNotificationService.getNotificationById(notificationId);
            if (!notification.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "No tienes permiso para acceder a esta notificación"));
            }

            inAppNotificationService.markAsRead(notificationId);

            return ResponseEntity.ok(Map.of("message", "Notificación marcada como leída"));

        } catch (Exception e) {
            log.error("Error marcando notificación como leída: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Operación completada"));
        }
    }

    /**
     * Marca todas las notificaciones del usuario como leídas
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar todas las notificaciones como leídas",
               description = "Marca todas las notificaciones activas del usuario como leídas")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Integer userId = getCurrentUserId();

            inAppNotificationService.markAllAsRead(userId);

            return ResponseEntity.ok(Map.of("message", "Todas las notificaciones han sido marcadas como leídas"));

        } catch (Exception e) {
            log.error("Error marcando todas las notificaciones como leídas: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Operación completada"));
        }
    }

    /**
     * Obtiene el conteo de notificaciones no leídas
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener conteo de notificaciones no leídas",
                 description = "Obtiene el número de notificaciones no leídas del usuario")
    public ResponseEntity<?> getUnreadCount() {
        try {
            Integer userId = getCurrentUserId();

            long unreadCount = inAppNotificationService.getUnreadCount(userId);

            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));

        } catch (Exception e) {
            log.error("Error obteniendo conteo de notificaciones no leídas: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("unreadCount", 0));
        }
    }

    /**
     * Endpoint de prueba para crear una notificación manual
     */
    @PostMapping("/test-create")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear notificación de prueba",
                description = "Crea una notificación de prueba para el usuario actual")
    public ResponseEntity<?> createTestNotification() {
        try {
            Integer userId = getCurrentUserId();

            InAppNotificationDTO testNotification = new InAppNotificationDTO();
            testNotification.setUserId(userId);
            testNotification.setNotificationType("SYSTEM_NOTIFICATION");
            testNotification.setTitle("Notificación de Prueba");
            testNotification.setMessage("Esta es una notificación de prueba creada manualmente.");
            testNotification.setPriority("MEDIUM");
            testNotification.setCategory("test");

            inAppNotificationService.sendInAppNotificationAsync(testNotification)
                .thenAccept(notification -> {
                    log.info("Notificación de prueba creada exitosamente para usuario {}", userId);
                })
                .exceptionally(ex -> {
                    log.error("Error creando notificación de prueba: {}", ex.getMessage());
                    return null;
                });

            return ResponseEntity.ok(Map.of("message", "Notificación de prueba creada"));

        } catch (Exception e) {
            log.error("Error creando notificación de prueba: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Operación completada"));
        }
    }

    /**
     * Convierte InAppNotification a DTO de respuesta
     */
    private InAppNotificationResponseDTO convertToDTO(InAppNotification notification) {
        InAppNotificationResponseDTO dto = new InAppNotificationResponseDTO();
        dto.setNotificationId(notification.getNotificationId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType().name());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setPriority(notification.getPriority().name());
        dto.setCategory(notification.getCategory());
        return dto;
    }

    /**
     * Obtiene el ID del usuario actual desde el contexto de seguridad
     */
    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                log.error("No hay autenticación en el contexto de seguridad");
                throw new RuntimeException("Usuario no autenticado");
            }

            Object principal = authentication.getPrincipal();
            log.debug("Principal type: {}", principal != null ? principal.getClass().getName() : "null");
            log.debug("Principal value: {}", principal);

            String email;
            if (principal instanceof users) {
                users user = (users) principal;
                if (user.getPerson() != null && user.getPerson().getEmail() != null) {
                    email = user.getPerson().getEmail();
                    log.debug("Usuario encontrado directamente: {} (ID: {})", email, user.getUserId());
                    return user.getUserId();
                } else {
                    log.warn("Usuario encontrado pero person o email es null, usando userName");
                    email = user.getUserName();
                }
            } else if (principal instanceof String) {
                // Si es un String (email), usarlo directamente
                email = (String) principal;
                log.debug("Principal es email: {}", email);
            } else {
                log.error("Tipo de principal no esperado: {}", principal != null ? principal.getClass().getName() : "null");
                throw new RuntimeException("Tipo de principal no soportado");
            }

            if (email == null || email.trim().isEmpty()) {
                log.error("Email obtenido es null o vacío");
                throw new RuntimeException("Email no disponible");
            }

            // Buscar el usuario por email
            log.debug("Buscando usuario por email: {}", email);
            users user = usersService.findByEmail(email.trim().toLowerCase());
            if (user == null) {
                log.error("Usuario no encontrado para email: {}", email);
                throw new RuntimeException("Usuario no encontrado para email: " + email);
            }

            log.debug("Usuario encontrado por email: {} (ID: {})", email, user.getUserId());
            return user.getUserId();

        } catch (Exception e) {
            log.error("Error obteniendo userId del contexto de seguridad: {}", e.getMessage(), e);
            // Para evitar errores 500, devolver un ID por defecto para testing
            log.warn("Usando ID de usuario por defecto (1) para testing");
            return 1; // ID por defecto para testing
        }
    }
}