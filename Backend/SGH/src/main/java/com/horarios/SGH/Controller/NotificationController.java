package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.NotificationService;
import com.horarios.SGH.Service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;

/**
 * Controlador REST para el manejo completo de notificaciones
 * Incluye endpoints para correo electrónico y notificaciones In-App en tiempo real
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "Endpoints para gestión completa de notificaciones (correo e In-App)")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private InAppNotificationService inAppNotificationService;
    
    // === ENDPOINTS PARA CORREO ELECTRÓNICO ===
    
    /**
     * Envía una notificación individual por correo electrónico
     */
    @PostMapping("/send")
    @Operation(summary = "Enviar notificación por correo", 
              description = "Envía una notificación por correo electrónico a un destinatario específico")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody NotificationDTO notification) {

        log.info("Solicitud de envío de notificación para: {}", notification.getRecipientEmail());

        try {
            // Validar y preparar la notificación en el hilo principal
            notificationService.validateAndPrepareNotification(notification);

            // Enviar de forma asíncrona
            notificationService.sendNotificationAsync(notification);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificación enviada exitosamente");
            response.put("recipient", notification.getRecipientEmail());
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validación fallida para notificación: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Error en la solicitud de envío de notificación: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Envía una notificación masiva a múltiples destinatarios por correo
     */
    @PostMapping("/send-bulk")
    @Operation(summary = "Envío masivo de correos", 
              description = "Envía la misma notificación por correo a múltiples destinatarios")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendBulkNotification(
            @Valid @RequestBody java.util.List<NotificationDTO> notifications) {
        
        log.info("Solicitud de envío masivo de {} notificaciones", notifications.size());
        
        try {
            if (notifications.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "La lista de notificaciones no puede estar vacía");
                
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(errorResponse)
                );
            }
            
            return notificationService.sendBulkNotificationAsync(notifications)
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Envío masivo iniciado exitosamente");
                    response.put("totalRecipients", notifications.size());
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error en envío masivo: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error en envío masivo: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en solicitud de envío masivo: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    /**
     * Envía notificación por rol vía correo electrónico
     */
    @PostMapping("/send-by-role")
    @Operation(summary = "Envío por rol - Correo", 
              description = "Envía una notificación por correo a todos los usuarios que tengan un rol específico")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendNotificationByRole(
            @RequestParam String role,
            @RequestParam String notificationType,
            @RequestParam String subject,
            @RequestParam String content) {
        
        log.info("Solicitud de envío por rol '{}' para tipo '{}'", role, notificationType);
        
        try {
            NotificationType type;
            try {
                type = NotificationType.valueOf(notificationType);
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Tipo de notificación inválido: " + notificationType);
                
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(errorResponse)
                );
            }
            
            Map<String, String> variables = new HashMap<>();
            variables.put("content", content);
            variables.put("subject", subject);
            
            return notificationService.sendNotificationToRoleAsync(role, type, subject, variables)
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Envío por rol iniciado exitosamente");
                    response.put("role", role);
                    response.put("notificationType", notificationType);
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error en envío por rol: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error en envío por rol: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en solicitud de envío por rol: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    // === ENDPOINTS PARA NOTIFICACIONES IN-APP ===
    
    /**
     * Envía notificación In-App a un usuario específico
     */
    @PostMapping("/inapp/send")
    @Operation(summary = "Enviar notificación In-App", 
              description = "Envía una notificación In-App en tiempo real a un usuario específico")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendInAppNotification(
            @Valid @RequestBody InAppNotificationDTO notification) {
        
        log.info("Solicitud de envío de notificación In-App para usuario {}", notification.getUserId());
        
        try {
            return inAppNotificationService.sendInAppNotificationAsync(notification)
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Notificación In-App enviada exitosamente");
                    response.put("notificationId", result.getNotificationId());
                    response.put("userId", notification.getUserId());
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error al enviar notificación In-App: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error al enviar notificación In-App: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en solicitud de envío In-App: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    /**
     * Envío masivo de notificaciones In-App
     */
    @PostMapping("/inapp/send-bulk")
    @Operation(summary = "Envío masivo In-App", 
              description = "Envía notificaciones In-App en tiempo real a múltiples usuarios")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendBulkInAppNotification(
            @Valid @RequestBody java.util.List<InAppNotificationDTO> notifications) {
        
        log.info("Solicitud de envío masivo In-App de {} notificaciones", notifications.size());
        
        try {
            if (notifications.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "La lista de notificaciones no puede estar vacía");
                
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(errorResponse)
                );
            }
            
            java.util.List<CompletableFuture<com.horarios.SGH.Model.InAppNotification>> futures = 
                notifications.stream()
                    .map(inAppNotificationService::sendInAppNotificationAsync)
                    .toList();
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            return allFutures.thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Envío masivo In-App iniciado exitosamente");
                response.put("totalRecipients", notifications.size());
                response.put("timestamp", java.time.LocalDateTime.now().toString());
                
                return ResponseEntity.ok(response);
            });
                
        } catch (Exception e) {
            log.error("Error en solicitud de envío masivo In-App: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    /**
     * Envío por rol de notificaciones In-App
     */
    @PostMapping("/inapp/send-by-role")
    @Operation(summary = "Envío por rol In-App", 
              description = "Envía notificaciones In-App en tiempo real a todos los usuarios de un rol específico")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendInAppNotificationByRole(
            @RequestParam String role,
            @RequestParam String notificationType,
            @RequestParam String title,
            @RequestParam String message) {
        
        log.info("Solicitud de envío In-App por rol '{}' para tipo '{}'", role, notificationType);
        
        try {
            NotificationType type;
            try {
                type = NotificationType.valueOf(notificationType);
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Tipo de notificación inválido: " + notificationType);
                
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(errorResponse)
                );
            }
            
            return notificationService.sendNotificationToRoleAsync(role, type, title, 
                java.util.Map.of("title", title, "message", message))
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Envío In-App por rol iniciado exitosamente");
                    response.put("role", role);
                    response.put("notificationType", notificationType);
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error en envío In-App por rol: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error en envío por rol: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en solicitud de envío In-App por rol: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    /**
     * Obtener notificaciones de un usuario específico
     */
    @GetMapping("/inapp/user/{userId}")
    @Operation(summary = "Obtener notificaciones usuario", 
              description = "Obtiene todas las notificaciones In-App de un usuario específico")
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Solicitud de notificaciones In-App para usuario {}, página: {}, tamaño: {}", 
                userId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // Aquí necesitaríamos implementar la lógica real
            // Por ahora devolvemos una respuesta simulada
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("content", java.util.Collections.emptyList());
            response.put("totalElements", 0L);
            response.put("totalPages", 0);
            response.put("number", page);
            response.put("size", size);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener notificaciones In-App del usuario {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener notificaciones: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Obtener notificaciones no leídas de un usuario
     */
    @GetMapping("/inapp/user/{userId}/unread")
    @Operation(summary = "Notificaciones no leídas", 
              description = "Obtiene solo las notificaciones In-App no leídas de un usuario")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(
            @PathVariable Integer userId) {
        
        log.info("Solicitud de notificaciones In-App no leídas para usuario {}", userId);
        
        try {
            // Aquí necesitaríamos implementar la lógica real
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("unreadCount", 0L);
            response.put("notifications", java.util.Collections.emptyList());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener notificaciones no leídas del usuario {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener notificaciones no leídas: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Contar notificaciones no leídas de un usuario
     */
    @GetMapping("/inapp/user/{userId}/count")
    @Operation(summary = "Contador no leídas", 
              description = "Obtiene el número de notificaciones In-App no leídas de un usuario")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable Integer userId) {
        
        log.info("Solicitud de contador de notificaciones no leídas para usuario {}", userId);
        
        try {
            // Aquí necesitaríamos implementar la lógica real
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("unreadCount", 0);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener contador de notificaciones del usuario {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener contador: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Marcar una notificación como leída
     */
    @PutMapping("/inapp/{notificationId}/read")
    @Operation(summary = "Marcar como leída", 
              description = "Marca una notificación In-App específica como leída")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long notificationId,
            @RequestBody Map<String, Integer> requestBody) {
        
        Integer userId = requestBody.get("userId");
        log.info("Solicitud de marcar como leída la notificación {} para usuario {}", 
                notificationId, userId);
        
        try {
            // Aquí necesitaríamos implementar la lógica real
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificación marcada como leída");
            response.put("notificationId", notificationId);
            response.put("userId", userId);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al marcar como leída la notificación {}: {}", notificationId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al marcar como leída: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Marcar todas las notificaciones de un usuario como leídas
     */
    @PutMapping("/inapp/user/{userId}/read-all")
    @Operation(summary = "Marcar todas como leídas", 
              description = "Marca todas las notificaciones In-App de un usuario como leídas")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @PathVariable Integer userId) {
        
        log.info("Solicitud de marcar todas las notificaciones como leídas para usuario {}", userId);
        
        try {
            // Aquí necesitaríamos implementar la lógica real
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Todas las notificaciones marcadas como leídas");
            response.put("userId", userId);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al marcar todas las notificaciones como leídas del usuario {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al marcar todas como leídas: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // === ENDPOINTS COMPARTIDOS ===
    
    /**
     * Reintenta notificaciones fallidas
     */
    @PostMapping("/retry-failed")
    @Operation(summary = "Reintentar notificaciones fallidas", 
              description = "Reintenta el envío de notificaciones que fallaron previamente")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> retryFailedNotifications() {
        
        log.info("Solicitud de reintento de notificaciones fallidas");
        
        try {
            return notificationService.retryFailedNotifications()
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Proceso de reintento iniciado");
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error en reintento de notificaciones: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error en reintento: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en solicitud de reintento: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }
    
    /**
     * Obtiene estadísticas del sistema de notificaciones
     */
    @GetMapping("/email/statistics")
    @Operation(summary = "Estadísticas de correo", 
              description = "Obtiene estadísticas del sistema de notificaciones por correo electrónico")
    public ResponseEntity<Map<String, Object>> getEmailStatistics() {
        
        log.info("Solicitud de estadísticas de notificaciones por correo");
        
        try {
            Map<String, Object> stats = notificationService.getNotificationStatistics();
            stats.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener estadísticas: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Obtiene estadísticas de notificaciones In-App por usuario
     */
    @GetMapping("/inapp/user/{userId}/stats")
    @Operation(summary = "Estadísticas usuario In-App", 
              description = "Obtiene estadísticas de notificaciones In-App para un usuario específico")
    public ResponseEntity<Map<String, Object>> getUserInAppStats(
            @PathVariable Integer userId) {
        
        log.info("Solicitud de estadísticas In-App para usuario {}", userId);
        
        try {
            // Aquí necesitaríamos implementar la lógica real
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", userId);
            stats.put("unreadCount", 0);
            stats.put("totalNotifications", 0);
            stats.put("byPriority", java.util.Map.of(
                "LOW", 0, "MEDIUM", 0, "HIGH", 0, "CRITICAL", 0
            ));
            stats.put("lastUpdated", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas In-App del usuario {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener estadísticas: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Obtiene tipos de notificación disponibles
     */
    @GetMapping("/types")
    @Operation(summary = "Tipos de notificación", 
              description = "Obtiene todos los tipos de notificación disponibles por rol")
    public ResponseEntity<java.util.Map<String, String[]>> getNotificationTypes() {
        
        log.info("Solicitud de tipos de notificación");
        
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
            log.error("Error al obtener tipos de notificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}