package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para las notificaciones In-App (notificaciones en la aplicación)
 * Utilizado para enviar notificaciones en tiempo real a React web y React Native móvil
 */
@Data
public class InAppNotificationDTO {
    
    private Long notificationId;
    
    @NotNull(message = "El ID de usuario es obligatorio")
    private Integer userId;
    
    @NotBlank(message = "El email del usuario es obligatorio")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    private String userEmail;
    
    @NotBlank(message = "El nombre del usuario es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String userName;
    
    @NotBlank(message = "El rol del usuario es obligatorio")
    @Size(max = 50, message = "El rol no puede exceder 50 caracteres")
    private String userRole;
    
    @NotNull(message = "El tipo de notificación es obligatorio")
    private NotificationType notificationType;
    
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede exceder 255 caracteres")
    private String title;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
    private String message;
    
    @Size(max = 500, message = "La URL de acción no puede exceder 500 caracteres")
    private String actionUrl;
    
    @Size(max = 100, message = "El texto de acción no puede exceder 100 caracteres")
    private String actionText;
    
    @Size(max = 100, message = "El icono no puede exceder 100 caracteres")
    private String icon;
    
    @NotNull(message = "La prioridad es obligatoria")
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    @Size(max = 50, message = "La categoría no puede exceder 50 caracteres")
    private String category = "general";
    
    private boolean isRead = false;
    private boolean isArchived = false;
    
    private LocalDateTime expiresAt;
    
    private Map<String, Object> metadata; // Datos adicionales
    
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    
    // Constructor vacío
    public InAppNotificationDTO() {}
    
    // Constructor con parámetros principales
    public InAppNotificationDTO(Integer userId, String userEmail, String userName, String userRole,
                               NotificationType notificationType, String title, String message) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userRole = userRole;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.priority = NotificationPriority.MEDIUM;
        this.category = "general";
    }
    
    /**
     * Obtiene el estado de prioridad en formato legible
     */
    public String getPriorityDisplayName() {
        return priority != null ? priority.getDisplayName() : "Media";
    }
    
    /**
     * Obtiene el color de prioridad para UI
     */
    public String getPriorityColor() {
        return priority != null ? priority.getColor() : "#17a2b8";
    }
    
    /**
     * Obtiene el icono de prioridad para UI
     */
    public String getPriorityIcon() {
        return priority != null ? priority.getIcon() : "🔔";
    }
    
    /**
     * Verifica si es una notificación reciente (menos de 24 horas)
     */
    public boolean isRecent() {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * Obtiene la antigüedad en formato legible
     */
    public String getAge() {
        if (createdAt == null) return "N/A";
        
        LocalDateTime now = LocalDateTime.now();
        if (createdAt.isAfter(now.minusMinutes(1))) {
            return "Hace un momento";
        } else if (createdAt.isAfter(now.minusMinutes(60))) {
            long minutes = java.time.Duration.between(createdAt, now).toMinutes();
            return "Hace " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        } else if (createdAt.isAfter(now.minusHours(24))) {
            long hours = java.time.Duration.between(createdAt, now).toHours();
            return "Hace " + hours + " hora" + (hours > 1 ? "s" : "");
        } else if (createdAt.isAfter(now.minusDays(7))) {
            long days = java.time.Duration.between(createdAt, now).toDays();
            return "Hace " + days + " día" + (days > 1 ? "s" : "");
        } else {
            return "Hace más de una semana";
        }
    }
    
    /**
     * Verifica si la notificación está activa
     */
    public boolean isActive() {
        return !isArchived && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    /**
     * Verifica si requiere atención inmediata
     */
    public boolean requiresImmediateAttention() {
        return priority != null && priority.requiresImmediateAttention();
    }
}