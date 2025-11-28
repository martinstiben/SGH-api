package com.horarios.SGH.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Modelo para notificaciones In-App (notificaciones en la aplicación)
 * Estas notificaciones se muestran en tiempo real en las interfaces web y móvil
 */
@Entity(name = "in_app_notifications")
@Data
public class InAppNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;
    
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;
    
    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    @Column(name = "action_text", length = 100)
    private String actionText;
    
    @Column(name = "icon", length = 100)
    private String icon; // URL o nombre del icono
    
    @Column(name = "priority", nullable = false)
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
    
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;
    
    @Column(name = "category", length = 50)
    private String category; // schedule, class, system, etc.
    
    @Column(name = "expires_at", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime expiresAt;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // Datos adicionales en formato JSON
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "read_at", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime readAt;
    
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
    
    // Constructor vacío
    public InAppNotification() {
        this.createdAt = LocalDateTime.now();
        this.priority = NotificationPriority.MEDIUM;
    }
    
    // Constructor con parámetros principales
    public InAppNotification(Integer userId, String userEmail, String userName, String userRole,
                           NotificationType notificationType, String title, String message) {
        this();
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userRole = userRole;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.category = "general";
    }
    
    /**
     * Marca la notificación como leída
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marca la notificación como archivada
     */
    public void markAsArchived() {
        this.isArchived = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Verifica si la notificación está activa (no expirada y no archivada)
     */
    public boolean isActive() {
        return !isArchived && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    /**
     * Verifica si la notificación es reciente (menos de 24 horas)
     */
    public boolean isRecent() {
        return createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * Obtiene la antigüedad en formato legible
     */
    public String getAge() {
        return getAgeString(this.createdAt);
    }
    
    /**
     * Obtiene la edad en formato legible desde una fecha
     */
    private String getAgeString(LocalDateTime fromDate) {
        LocalDateTime now = LocalDateTime.now();
        if (fromDate.isAfter(now.minusMinutes(1))) {
            return "Hace un momento";
        } else if (fromDate.isAfter(now.minusMinutes(60))) {
            return "Hace " + (now.getMinute() - fromDate.getMinute()) + " minutos";
        } else if (fromDate.isAfter(now.minusHours(24))) {
            return "Hace " + (now.getHour() - fromDate.getHour()) + " horas";
        } else if (fromDate.isAfter(now.minusDays(7))) {
            return "Hace " + (now.getDayOfYear() - fromDate.getDayOfYear()) + " días";
        } else {
            return "Hace más de una semana";
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "InAppNotification{id=%d, user='%s', type=%s, title='%s', priority=%s, isRead=%s}",
            notificationId, userEmail, notificationType, title, priority, isRead
        );
    }
}