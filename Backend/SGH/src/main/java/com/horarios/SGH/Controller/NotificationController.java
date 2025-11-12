package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationLog;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
 * Controlador REST para el manejo de notificaciones por correo electrónico
 * Proporciona endpoints para enviar notificaciones, ver logs y gestionar el sistema de alertas
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "Endpoints para gestión de notificaciones por correo electrónico")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Envía una notificación individual por correo electrónico
     */
    @PostMapping("/send")
    @Operation(summary = "Enviar notificación individual", 
              description = "Envía una notificación por correo electrónico a un destinatario específico")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendNotification(
            @Valid @RequestBody NotificationDTO notification) {
        
        log.info("Solicitud de envío de notificación para: {}", notification.getRecipientEmail());
        
        try {
            return notificationService.sendNotificationAsync(notification)
                .thenApply(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Notificación enviada exitosamente");
                    response.put("recipient", notification.getRecipientEmail());
                    response.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error al enviar notificación: {}", ex.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error al enviar notificación: " + ex.getMessage());
                    errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
                
        } catch (Exception e) {
            log.error("Error en la solicitud de envío de notificación: {}", e.getMessage());
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
     * Envía una notificación masiva a múltiples destinatarios
     */
    @PostMapping("/send-bulk")
    @Operation(summary = "Envío masivo de notificaciones", 
              description = "Envía la misma notificación a múltiples destinatarios")
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
     * Envía notificación a todos los usuarios de un rol específico
     */
    @PostMapping("/send-by-role")
    @Operation(summary = "Envío por rol", 
              description = "Envía una notificación a todos los usuarios que tengan un rol específico")
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
     * Obtiene estadísticas de notificaciones
     */
    @GetMapping("/statistics")
    @Operation(summary = "Estadísticas de notificaciones", 
              description = "Obtiene estadísticas del sistema de notificaciones")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        
        log.info("Solicitud de estadísticas de notificaciones");
        
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
     * Obtiene logs de notificaciones con paginación
     */
    @GetMapping("/logs")
    @Operation(summary = "Logs de notificaciones", 
              description = "Obtiene el historial de notificaciones con paginación")
    public ResponseEntity<Page<NotificationLog>> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Solicitud de logs de notificaciones - página: {}, tamaño: {}, ordenamiento: {} {}", 
                page, size, sortBy, sortDir);
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Aquí necesitaríamos inyectar el repositorio para obtener los logs
            // Por ahora devolvemos una página vacía
            Page<NotificationLog> emptyPage = Page.empty(pageable);
            
            return ResponseEntity.ok(emptyPage);
            
        } catch (Exception e) {
            log.error("Error al obtener logs de notificaciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtiene tipos de notificación disponibles
     */
    @GetMapping("/types")
    @Operation(summary = "Tipos de notificación", 
              description = "Obtiene todos los tipos de notificación disponibles")
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
                "COORDINATOR_MAINTENANCE_ALERT"
            });
            
            types.put("GENERAL", new String[]{
                "GENERAL_SYSTEM_NOTIFICATION"
            });
            
            return ResponseEntity.ok(types);
            
        } catch (Exception e) {
            log.error("Error al obtener tipos de notificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}