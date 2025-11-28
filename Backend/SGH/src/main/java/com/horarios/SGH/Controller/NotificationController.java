package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationStatus;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para la gestión de notificaciones por correo electrónico
 * Proporciona endpoints para enviar notificaciones, consultar logs y estadísticas
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "API para gestión de notificaciones por correo electrónico")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Envía una notificación individual
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Enviar notificación individual",
               description = "Envía una notificación por correo electrónico a un destinatario específico")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationDTO notification) {
        try {
            log.info("Solicitud de envío de notificación a: {}", notification.getRecipientEmail());

            // Validar y preparar la notificación
            notificationService.validateAndPrepareNotification(notification);

            // Enviar de forma asíncrona
            CompletableFuture<Void> future = notificationService.sendNotificationAsync(notification);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Notificación enviada exitosamente",
                        "recipient", notification.getRecipientEmail(),
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificación: " + e.getMessage()));
        }
    }

    /**
     * Envía notificación masiva
     */
    @PostMapping("/send/bulk")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Enviar notificaciones masivas",
               description = "Envía notificaciones por correo electrónico a múltiples destinatarios")
    public ResponseEntity<?> sendBulkNotifications(@RequestBody List<NotificationDTO> notifications) {
        try {
            log.info("Solicitud de envío masivo de {} notificaciones", notifications.size());

            CompletableFuture<Void> future = notificationService.sendBulkNotificationAsync(notifications);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Envío masivo iniciado exitosamente",
                        "totalNotifications", notifications.size(),
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificaciones masivas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificaciones masivas: " + e.getMessage()));
        }
    }

    /**
     * Envía notificación a todos los usuarios de un rol
     */
    @PostMapping("/send/role/{role}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Enviar notificación por rol",
               description = "Envía una notificación a todos los usuarios de un rol específico")
    public ResponseEntity<?> sendNotificationToRole(
            @PathVariable String role,
            @RequestParam String subject,
            @RequestParam NotificationType type,
            @RequestBody(required = false) Map<String, String> variables) {

        try {
            log.info("Solicitud de envío de notificación a rol: {}", role);

            CompletableFuture<Void> future = notificationService.sendNotificationToRoleAsync(role, type, subject, variables);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Envío a rol iniciado exitosamente",
                        "role", role,
                        "notificationType", type,
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificación a rol {}: {}", role, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificación a rol: " + e.getMessage()));
        }
    }

    /**
     * Reintenta notificaciones fallidas
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Reintentar notificaciones fallidas",
               description = "Reintenta el envío de todas las notificaciones que fallaron anteriormente")
    public ResponseEntity<?> retryFailedNotifications() {
        try {
            log.info("Solicitud de reintento de notificaciones fallidas");

            CompletableFuture<Void> future = notificationService.retryFailedNotifications();

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Reintento de notificaciones fallidas iniciado",
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al reintentar notificaciones fallidas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al reintentar notificaciones: " + e.getMessage()));
        }
    }

    /**
     * Obtiene estadísticas de notificaciones
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener estadísticas de notificaciones",
               description = "Obtiene estadísticas generales del sistema de notificaciones")
    public ResponseEntity<?> getNotificationStats() {
        try {
            Map<String, Object> stats = notificationService.getNotificationStatistics();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    /**
     * Obtiene logs de notificaciones con paginación
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener logs de notificaciones",
               description = "Obtiene el historial de notificaciones con opciones de filtrado y paginación")
    public ResponseEntity<?> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String recipientEmail,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String recipientRole) {

        try {
            Pageable pageable = PageRequest.of(page, size);

            // Aquí iría la lógica para filtrar los logs según los parámetros
            // Por simplicidad, retornamos una respuesta básica
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Endpoint de logs implementado",
                "page", page,
                "size", size,
                "filters", Map.of(
                    "recipientEmail", recipientEmail,
                    "type", type,
                    "status", status,
                    "recipientRole", recipientRole
                )
            ));

        } catch (Exception e) {
            log.error("Error al obtener logs de notificaciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener logs: " + e.getMessage()));
        }
    }

    /**
     * Obtiene tipos de notificación disponibles para un rol
     */
    @GetMapping("/types/{role}")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener tipos de notificación por rol",
                description = "Obtiene los tipos de notificación disponibles para un rol específico")
    public ResponseEntity<?> getNotificationTypesForRole(@PathVariable String role) {
        try {
            java.util.Map<String, String[]> types = new HashMap<>();

            // Agrupar tipos por rol
            types.put("ESTUDIANTE", new String[]{
                "STUDENT_SCHEDULE_ASSIGNMENT",
                "STUDENT_SCHEDULE_CHANGE",
                "STUDENT_CLASS_CANCELLATION"
            });

            types.put("MAESTRO", new String[]{
                "TEACHER_CLASS_SCHEDULED",
                "TEACHER_CLASS_MODIFIED",
                "TEACHER_CLASS_CANCELLED",
                "TEACHER_AVAILABILITY_CHANGED"
            });

            types.put("DIRECTOR_DE_AREA", new String[]{
                "DIRECTOR_SCHEDULE_CONFLICT",
                "DIRECTOR_AVAILABILITY_ISSUE",
                "DIRECTOR_SYSTEM_INCIDENT"
            });

            types.put("COORDINADOR", new String[]{
                "COORDINATOR_GLOBAL_UPDATE",
                "COORDINATOR_SYSTEM_ALERT",
                "COORDINATOR_CHANGE_CONFIRMATION",
                "COORDINATOR_MAINTENANCE_ALERT",
                "COORDINATOR_USER_REGISTRATION_PENDING",
                "COORDINATOR_USER_APPROVED",
                "COORDINATOR_USER_REJECTED"
            });

            types.put("GENERAL", new String[]{
                "GENERAL_SYSTEM_NOTIFICATION",
                "USER_REGISTRATION_APPROVED",
                "USER_REGISTRATION_REJECTED"
            });

            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error al obtener tipos de notificación para rol {}: {}", role, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener tipos: " + e.getMessage()));
        }
    }






}