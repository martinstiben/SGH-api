package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que registra el historial de envíos de notificaciones por correo electrónico en el sistema SGH.
 * Cada instancia representa un intento de envío de notificación, incluyendo estado,
 * intentos de reenvío, errores y métricas de rendimiento.
 *
 * Esta entidad es crucial para el monitoreo y debugging del sistema de notificaciones,
 * permitiendo rastrear el éxito de los envíos y diagnosticar problemas.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de registrar logs de notificaciones
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractEntity
 *
 * Patrones de diseño aplicados:
 * - Template Method: Implementado a través de AbstractEntity
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name = "notification_logs")
public class NotificationLog extends AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;
    
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

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

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public NotificationLog() {
        super();
        this.lastAttempt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales para crear un log de notificación.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param recipientEmail email del destinatario
     * @param notificationType tipo de notificación
     * @param subject asunto de la notificación
     * @param content contenido de la notificación
     */
    public NotificationLog(String recipientEmail, NotificationType notificationType, String subject, String content) {
        super();
        this.recipientEmail = recipientEmail;
        this.notificationType = notificationType;
        this.subject = subject;
        this.content = content;
        this.status = NotificationStatus.PENDING;
        this.lastAttempt = LocalDateTime.now();
    }

    /**
     * Obtiene el identificador único del log.
     *
     * @return ID del log
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * Establece el identificador único del log.
     *
     * @param logId ID del log
     */
    public void setLogId(Long logId) {
        this.logId = logId;
    }

    /**
     * Obtiene el email del destinatario.
     *
     * @return email del destinatario
     */
    public String getRecipientEmail() {
        return recipientEmail;
    }

    /**
     * Establece el email del destinatario.
     *
     * @param recipientEmail email del destinatario
     */
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    /**
     * Obtiene el tipo de notificación.
     *
     * @return tipo de notificación
     */
    public NotificationType getNotificationType() {
        return notificationType;
    }

    /**
     * Establece el tipo de notificación.
     *
     * @param notificationType tipo de notificación
     */
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    /**
     * Obtiene el asunto de la notificación.
     *
     * @return asunto de la notificación
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Establece el asunto de la notificación.
     *
     * @param subject asunto de la notificación
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Obtiene el contenido de la notificación.
     *
     * @return contenido de la notificación
     */
    public String getContent() {
        return content;
    }

    /**
     * Establece el contenido de la notificación.
     *
     * @param content contenido de la notificación
     */
    public void setContent(String content) {
        this.content = content;
    }


    /**
     * Obtiene el estado actual del envío.
     *
     * @return estado del envío
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * Establece el estado actual del envío.
     *
     * @param status estado del envío
     */
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    /**
     * Obtiene el número de intentos realizados.
     *
     * @return número de intentos
     */
    public Integer getAttemptsCount() {
        return attemptsCount;
    }

    /**
     * Establece el número de intentos realizados.
     *
     * @param attemptsCount número de intentos
     */
    public void setAttemptsCount(Integer attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    /**
     * Obtiene el número máximo de intentos permitidos.
     *
     * @return número máximo de intentos
     */
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Establece el número máximo de intentos permitidos.
     *
     * @param maxAttempts número máximo de intentos
     */
    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Obtiene el mensaje de error del último intento fallido.
     *
     * @return mensaje de error
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Establece el mensaje de error del último intento fallido.
     *
     * @param errorMessage mensaje de error
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Obtiene la fecha del último intento de envío.
     *
     * @return fecha del último intento
     */
    public LocalDateTime getLastAttempt() {
        return lastAttempt;
    }

    /**
     * Establece la fecha del último intento de envío.
     *
     * @param lastAttempt fecha del último intento
     */
    public void setLastAttempt(LocalDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    /**
     * Obtiene la fecha en que la notificación fue enviada exitosamente.
     *
     * @return fecha de envío exitoso
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    /**
     * Establece la fecha en que la notificación fue enviada exitosamente.
     *
     * @param sentAt fecha de envío exitoso
     */
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * Obtiene la fecha de creación del log.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación del log.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de última actualización del log.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización del log.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
    
    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios del log de notificación sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("El email del destinatario es obligatorio");
        }
        if (notificationType == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido es obligatorio");
        }
        if (maxAttempts == null || maxAttempts < 1) {
            throw new IllegalArgumentException("El número máximo de intentos debe ser al menos 1");
        }
    }

    /**
     * Obtiene una representación resumida del log de notificación.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return String.format("Log %s: %s - %s (%d/%d intentos) - %s",
                notificationType != null ? notificationType.name() : "Sin tipo",
                recipientEmail != null ? recipientEmail : "Sin destinatario",
                subject != null ? subject : "Sin asunto",
                attemptsCount != null ? attemptsCount : 0,
                maxAttempts != null ? maxAttempts : 0,
                status != null ? status.name() : "Sin estado");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return logId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string del log de notificación
     */
    @Override
    public String toString() {
        return String.format(
            "NotificationLog{id=%d, recipient='%s', type=%s, status=%s, attempts=%d/%d, elapsed=%s, createdAt=%s}",
            logId, recipientEmail, notificationType, status, attemptsCount, maxAttempts, 
            getElapsedTime(), getCreatedAt()
        );
    }
}