package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa las notificaciones In-App del sistema SGH.
 * Estas notificaciones se muestran en tiempo real en las interfaces web y móvil,
 * permitiendo comunicación inmediata con los usuarios sobre eventos del sistema.
 *
 * Las notificaciones pueden ser de diferentes tipos, prioridades y categorías,
 * y soportan acciones interactivas como URLs y textos de acción.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar notificaciones In-App
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
@Entity(name = "in_app_notifications")
public class InAppNotification extends AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "action_url", length = 255)
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private NotificationCategory category = NotificationCategory.GENERAL;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "read_at", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime readAt;
    
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public InAppNotification() {
        super();
        this.priority = NotificationPriority.MEDIUM;
    }

    /**
     * Constructor con parámetros principales para crear una notificación.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param userId ID del usuario destinatario
     * @param notificationType tipo de notificación
     * @param title título de la notificación
     * @param message mensaje de la notificación
     */
    public InAppNotification(Long userId, NotificationType notificationType, String title, String message) {
        super();
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.category = NotificationCategory.GENERAL;
    }

    /**
     * Obtiene el identificador único de la notificación.
     *
     * @return ID de la notificación
     */
    public Long getNotificationId() {
        return notificationId;
    }

    /**
     * Establece el identificador único de la notificación.
     *
     * @param notificationId ID de la notificación
     */
    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * Obtiene el ID del usuario destinatario.
     *
     * @return ID del usuario
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Establece el ID del usuario destinatario.
     *
     * @param userId ID del usuario
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
     * Obtiene el título de la notificación.
     *
     * @return título de la notificación
     */
    public String getTitle() {
        return title;
    }

    /**
     * Establece el título de la notificación.
     *
     * @param title título de la notificación
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene el mensaje de la notificación.
     *
     * @return mensaje de la notificación
     */
    public String getMessage() {
        return message;
    }

    /**
     * Establece el mensaje de la notificación.
     *
     * @param message mensaje de la notificación
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Obtiene la URL de acción de la notificación.
     *
     * @return URL de acción
     */
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * Establece la URL de acción de la notificación.
     *
     * @param actionUrl URL de acción
     */
    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    /**
     * Obtiene el texto de acción de la notificación.
     *
     * @return texto de acción
     */
    public String getActionText() {
        return actionText;
    }

    /**
     * Establece el texto de acción de la notificación.
     *
     * @param actionText texto de acción
     */
    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    /**
     * Obtiene el icono de la notificación.
     *
     * @return icono de la notificación
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Establece el icono de la notificación.
     *
     * @param icon icono de la notificación
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Obtiene la prioridad de la notificación.
     *
     * @return prioridad de la notificación
     */
    public NotificationPriority getPriority() {
        return priority;
    }

    /**
     * Establece la prioridad de la notificación.
     *
     * @param priority prioridad de la notificación
     */
    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    /**
     * Verifica si la notificación ha sido leída.
     *
     * @return true si ha sido leída
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Establece si la notificación ha sido leída.
     *
     * @param read true si ha sido leída
     */
    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Verifica si la notificación está archivada.
     *
     * @return true si está archivada
     */
    public boolean isArchived() {
        return isArchived;
    }

    /**
     * Establece si la notificación está archivada.
     *
     * @param archived true si está archivada
     */
    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    /**
     * Obtiene la categoría de la notificación.
     *
     * @return categoría de la notificación
     */
    public NotificationCategory getCategory() {
        return category;
    }

    /**
     * Establece la categoría de la notificación.
     *
     * @param category categoría de la notificación
     */
    public void setCategory(NotificationCategory category) {
        this.category = category;
    }


    /**
     * Obtiene la fecha de creación de la notificación.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación de la notificación.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha en que la notificación fue leída.
     *
     * @return fecha de lectura
     */
    public LocalDateTime getReadAt() {
        return readAt;
    }

    /**
     * Establece la fecha en que la notificación fue leída.
     *
     * @param readAt fecha de lectura
     */
    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    /**
     * Obtiene la fecha de última actualización de la notificación.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización de la notificación.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
     * Verifica si la notificación está activa (no archivada)
     */
    public boolean isActive() {
        return !isArchived;
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
    
    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios de la notificación sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        if (notificationType == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        if (priority == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }
    }

    /**
     * Obtiene una representación resumida de la notificación.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return String.format("Notificación %s: %s - %s (%s)",
                notificationType != null ? notificationType.name() : "Sin tipo",
                title != null ? title : "Sin título",
                userId != null ? "Usuario " + userId : "Sin usuario",
                priority != null ? priority.name() : "Sin prioridad");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return notificationId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string de la notificación
     */
    @Override
    public String toString() {
        return String.format(
            "InAppNotification{id=%d, userId=%d, type=%s, title='%s', priority=%s, isRead=%s, createdAt=%s}",
            notificationId, userId, notificationType, title, priority, isRead, getCreatedAt()
        );
    }
}