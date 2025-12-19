package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta para notificaciones In-App
 * Incluye todos los campos de respuesta que se muestran en Swagger
 */
/**
 * DTO de respuesta para notificaciones In-App del sistema SGH.
 * Implementa validaciones de negocio específicas para respuestas de notificación
 * y métodos de utilidad para gestión de estados de lectura.
 *
 * Proporciona métodos Factory para crear respuestas de notificación
 * y validaciones de campos requeridos.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de notificación In-App")
public class InAppNotificationResponseDTO extends AbstractDTO {

    @Schema(description = "ID único de la notificación", example = "123")
    private Long notificationId;

    @Schema(description = "ID del usuario destinatario", example = "1")
    private Long userId;

    @Schema(description = "Correo electrónico del usuario", example = "estudiante@universidad.edu")
    private String userEmail;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García")
    private String userName;

    @Schema(description = "Rol del usuario", example = "ESTUDIANTE")
    private String userRole;

    @Schema(description = "Tipo de notificación", example = "STUDENT_SCHEDULE_ASSIGNMENT")
    private String notificationType;

    @Schema(description = "Título de la notificación", example = "📚 Nuevo Horario Asignado")
    private String title;

    @Schema(description = "Mensaje de la notificación", example = "Se ha asignado un nuevo horario para el semestre 2025-1.")
    private String message;

    @Schema(description = "URL de acción (opcional)", example = "/horarios")
    private String actionUrl;

    @Schema(description = "Texto del botón de acción", example = "Ver Horario")
    private String actionText;

    @Schema(description = "Icono de la notificación", example = "📚")
    private String icon;

    @Schema(description = "Prioridad de la notificación", example = "MEDIUM")
    private String priority;

    @Schema(description = "Categoría de la notificación", example = "schedule")
    private String category;

    @Schema(description = "Indica si la notificación fue leída", example = "false")
    private boolean read;

    @Schema(description = "Indica si la notificación está archivada", example = "false")
    private boolean archived;

    @Schema(description = "Fecha de expiración (opcional)")
    private LocalDateTime expiresAt;

    @Schema(description = "Datos adicionales en formato JSON")
    private Map<String, Object> metadata;

    @Schema(description = "Fecha de creación", example = "2025-11-12T21:15:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    @Schema(description = "Fecha de lectura (opcional)")
    private LocalDateTime readAt;

    // Campos calculados para UI
    @Schema(description = "Nombre legible de la prioridad", example = "Media")
    private String priorityDisplayName;

    @Schema(description = "Color de la prioridad para UI", example = "#17a2b8")
    private String priorityColor;

    @Schema(description = "Icono de la prioridad para UI", example = "🔔")
    private String priorityIcon;

    @Schema(description = "Antigüedad en formato legible", example = "Hace 2 horas")
    private String age;

    @Schema(description = "Indica si es una notificación reciente", example = "true")
    private boolean recent;

    @Schema(description = "Indica si la notificación está activa", example = "true")
    private boolean active;

    @Schema(description = "Indica si requiere atención inmediata", example = "false")
    private boolean requiresImmediateAttention;

    // Getters y Setters
    /**
     * Constructor por defecto.
     */
    public InAppNotificationResponseDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param notificationId ID de la notificación
     * @param userId ID del usuario
     * @param title Título
     * @param message Mensaje
     */
    public InAppNotificationResponseDTO(Long notificationId, Long userId, String title, String message) {
        super();
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una respuesta básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param notificationId ID de la notificación
     * @param userId ID del usuario
     * @param title Título
     * @param message Mensaje
     * @return InAppNotificationResponseDTO configurado
     */
    public static InAppNotificationResponseDTO create(Long notificationId, Long userId, String title, String message) {
        InAppNotificationResponseDTO dto = new InAppNotificationResponseDTO();
        dto.setNotificationId(notificationId);
        dto.setUserId(userId);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un InAppNotificationResponseDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return InAppNotificationResponseDTO con valores por defecto
     */
    public static InAppNotificationResponseDTO empty() {
        InAppNotificationResponseDTO dto = new InAppNotificationResponseDTO();
        dto.setCreatedAt(LocalDateTime.now());
        dto.setRead(false);
        dto.setArchived(false);
        dto.setActive(true);
        return dto;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getPriorityDisplayName() {
        return priorityDisplayName;
    }

    public void setPriorityDisplayName(String priorityDisplayName) {
        this.priorityDisplayName = priorityDisplayName;
    }

    public String getPriorityColor() {
        return priorityColor;
    }

    public void setPriorityColor(String priorityColor) {
        this.priorityColor = priorityColor;
    }

    public String getPriorityIcon() {
        return priorityIcon;
    }

    public void setPriorityIcon(String priorityIcon) {
        this.priorityIcon = priorityIcon;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean recent) {
        this.recent = recent;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isRequiresImmediateAttention() {
        return requiresImmediateAttention;
    }

    public void setRequiresImmediateAttention(boolean requiresImmediateAttention) {
        this.requiresImmediateAttention = requiresImmediateAttention;
    }

    /**
     * Valida si el DTO tiene información básica completa.
     * Método de validación de negocio.
     *
     * @return true si tiene la información esencial
     */
    @Override
    public boolean isValid() {
        return notificationId != null && notificationId > 0 &&
               userId != null && userId > 0 &&
               title != null && !title.trim().isEmpty() &&
               message != null && !message.trim().isEmpty() &&
               notificationType != null && !notificationType.trim().isEmpty();
    }

    /**
     * Obtiene una representación resumida de la respuesta.
     * Formato: "Notificación [notificationId] - [title] - Usuario: [userId]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Notificación %d - %s - Usuario: %d",
                notificationId != null ? notificationId : 0,
                title != null ? title : "Sin título",
                userId != null ? userId : 0);
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}