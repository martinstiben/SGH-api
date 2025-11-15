package com.horarios.SGH.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo para el logging de notificaciones por correo electrónico
 * Registra el estado de cada envío de notificación en el sistema SGH
 */
@Entity(name = "notification_logs")
@Data
public class NotificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;
    
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;
    
    @Column(name = "recipient_role", nullable = false, length = 50)
    private String recipientRole;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;
    
    @Column(name = "subject", nullable = false, length = 500)
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "template_path", length = 500)
    private String templatePath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;
    
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "last_attempt", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime lastAttempt;
    
    @Column(name = "sent_at", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
    
    @ElementCollection
    @CollectionTable(name = "notification_log_variables", 
                     joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "variable_name")
    private List<String> templateVariables = new ArrayList<>();
    
    // Constructor vacío
    public NotificationLog() {
        this.createdAt = LocalDateTime.now();
        this.lastAttempt = LocalDateTime.now();
    }
    
    // Constructor con parámetros principales
    public NotificationLog(String recipientEmail, String recipientName, String recipientRole,
                          NotificationType notificationType, String subject, String content) {
        this();
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.recipientRole = recipientRole;
        this.notificationType = notificationType;
        this.subject = subject;
        this.content = content;
        this.status = NotificationStatus.PENDING;
    }
    
    /**
     * Incrementa el contador de intentos y registra la hora del último intento
     */
    public void incrementAttempts() {
        this.attemptsCount++;
        this.lastAttempt = LocalDateTime.now();
    }
    
    /**
     * Marca la notificación como enviada exitosamente
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    /**
     * Marca la notificación como fallida con mensaje de error
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Verifica si se puede intentar enviar nuevamente
     */
    public boolean canRetry() {
        return this.attemptsCount < this.maxAttempts && 
               this.status == NotificationStatus.PENDING;
    }
    
    /**
     * Verifica si el envío fue exitoso
     */
    public boolean isSent() {
        return this.status == NotificationStatus.SENT;
    }
    
    /**
     * Verifica si el envío falló definitivamente
     */
    public boolean isFailed() {
        return this.status == NotificationStatus.FAILED;
    }
    
    /**
     * Obtiene el tiempo transcurrido desde la creación
     */
    public String getElapsedTime() {
        if (sentAt != null) {
            return formatDuration(java.time.Duration.between(createdAt, sentAt).toMillis());
        } else if (lastAttempt != null) {
            return formatDuration(java.time.Duration.between(createdAt, LocalDateTime.now()).toMillis());
        } else {
            return "0s";
        }
    }
    
    /**
     * Formatea la duración en milisegundos a formato legible
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %02dh", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "NotificationLog{id=%d, recipient='%s', type=%s, status=%s, attempts=%d/%d, elapsed=%s}",
            logId, recipientEmail, notificationType, status, attemptsCount, maxAttempts, getElapsedTime()
        );
    }
}