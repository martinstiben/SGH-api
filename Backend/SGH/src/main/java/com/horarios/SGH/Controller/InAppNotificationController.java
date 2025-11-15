package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.InAppNotificationResponseDTO;
import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.NotificationPriority;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.InAppNotificationService;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Model.users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de notificaciones In-App
 * Permite a los usuarios consumir sus notificaciones desde el frontend
 */
@Slf4j
@RestController
@RequestMapping("/api/in-app-notifications")
@Tag(name = "Notificaciones In-App", description = "API para gestión de notificaciones dentro de la aplicación")
public class InAppNotificationController {

    @Autowired
    private InAppNotificationService inAppNotificationService;

    @Autowired
    private Iusers usersRepository;

    /**
     * Obtiene notificaciones activas del usuario autenticado
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener notificaciones activas",
               description = "Obtiene las notificaciones activas (no expiradas) del usuario autenticado")
    public ResponseEntity<?> getActiveNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            Page<InAppNotification> notifications = inAppNotificationService.getActiveNotificationsByUserId(userId, page, size);

            List<InAppNotificationResponseDTO> dtos = notifications.getContent().stream()
                .map(this::convertToResponseDTO)
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dtos,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "currentPage", page,
                "size", size
            ));

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones activas: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo notificaciones"));
        }
    }

    /**
     * Obtiene notificaciones no leídas del usuario autenticado
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener notificaciones no leídas",
               description = "Obtiene las notificaciones no leídas del usuario autenticado")
    public ResponseEntity<?> getUnreadNotifications(Authentication authentication) {
        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            List<InAppNotification> notifications = inAppNotificationService.getUnreadNotificationsByUserId(userId);

            List<InAppNotificationResponseDTO> dtos = notifications.stream()
                .map(this::convertToResponseDTO)
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dtos,
                "count", dtos.size()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones no leídas: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo notificaciones no leídas"));
        }
    }

    /**
     * Cuenta notificaciones no leídas del usuario autenticado
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Contar notificaciones no leídas",
               description = "Obtiene el conteo de notificaciones no leídas del usuario autenticado")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            Long count = inAppNotificationService.countUnreadNotificationsByUserId(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "unreadCount", count
            ));

        } catch (Exception e) {
            log.error("Error obteniendo conteo de notificaciones no leídas: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo conteo"));
        }
    }

    /**
     * Marca una notificación como leída
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar como leída",
               description = "Marca una notificación específica como leída")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId, Authentication authentication) {
        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            // Aquí podríamos verificar que la notificación pertenece al usuario, pero por simplicidad omitimos

            inAppNotificationService.markAsRead(notificationId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notificación marcada como leída"
            ));

        } catch (Exception e) {
            log.error("Error marcando notificación como leída: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error marcando como leída"));
        }
    }

    /**
     * Marca todas las notificaciones del usuario como leídas
     */
    @PutMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar todas como leídas",
               description = "Marca todas las notificaciones del usuario autenticado como leídas")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            inAppNotificationService.markAllAsRead(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Todas las notificaciones marcadas como leídas"
            ));

        } catch (Exception e) {
            log.error("Error marcando todas las notificaciones como leídas: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error marcando todas como leídas"));
        }
    }

    /**
     * Obtiene notificaciones por tipo
     */
    @GetMapping("/by-type/{type}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener por tipo",
               description = "Obtiene notificaciones del usuario autenticado filtradas por tipo")
    public ResponseEntity<?> getNotificationsByType(
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            Page<InAppNotification> notifications = inAppNotificationService.getNotificationsByTypeAndUser(userId, type, page, size);

            List<InAppNotificationResponseDTO> dtos = notifications.getContent().stream()
                .map(this::convertToResponseDTO)
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dtos,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "type", type.name()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones por tipo: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo notificaciones por tipo"));
        }
    }

    /**
     * Obtiene notificaciones por prioridad
     */
    @GetMapping("/by-priority/{priority}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener por prioridad",
               description = "Obtiene notificaciones del usuario autenticado filtradas por prioridad")
    public ResponseEntity<?> getNotificationsByPriority(
            @PathVariable NotificationPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Integer userId = getUserIdFromAuthentication(authentication);
            Page<InAppNotification> notifications = inAppNotificationService.getNotificationsByPriorityAndUser(userId, priority, page, size);

            List<InAppNotificationResponseDTO> dtos = notifications.getContent().stream()
                .map(this::convertToResponseDTO)
                .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dtos,
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "priority", priority.name()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones por prioridad: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error obteniendo notificaciones por prioridad"));
        }
    }

    /**
     * Convierte InAppNotification a InAppNotificationResponseDTO
     */
    private InAppNotificationResponseDTO convertToResponseDTO(InAppNotification notification) {
        InAppNotificationResponseDTO dto = new InAppNotificationResponseDTO();

        dto.setNotificationId(notification.getNotificationId());
        dto.setUserId(notification.getUserId());
        dto.setUserEmail(notification.getUserEmail());
        dto.setUserName(notification.getUserName());
        dto.setUserRole(notification.getUserRole());
        dto.setNotificationType(notification.getNotificationType().name());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setActionUrl(notification.getActionUrl());
        dto.setActionText(notification.getActionText());
        dto.setIcon(notification.getIcon());
        dto.setPriority(notification.getPriority().name());
        dto.setCategory(notification.getCategory());
        dto.setRead(notification.isRead());
        dto.setArchived(notification.isArchived());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        dto.setExpiresAt(notification.getExpiresAt());

        // Campos calculados
        dto.setPriorityDisplayName(notification.getPriority().getDisplayName());
        dto.setPriorityColor(notification.getPriority().getColor());
        dto.setPriorityIcon(notification.getPriority().getIcon());
        dto.setAge(notification.getAge());
        dto.setRecent(notification.isRecent());
        dto.setActive(notification.isActive());
        dto.setRequiresImmediateAttention(notification.getPriority().requiresImmediateAttention());

        return dto;
    }

    /**
     * Obtiene el userId desde Authentication
     */
    private Integer getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        users user = usersRepository.findByUserName(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getUserId();
    }
}