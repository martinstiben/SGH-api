package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * DTO para el envío de notificaciones In-App del sistema SGH.
 * Implementa enumeraciones para categorizar tipos de notificaciones
 * y prioridades, con métodos de utilidad para validación.
 *
 * Proporciona métodos de utilidad para validación y construcción
 * de notificaciones personalizadas según el rol del usuario.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos para enviar una notificación In-App")
public class InAppNotificationDTO extends AbstractDTO {

    /**
     * Identificador único del usuario destinatario.
     */
    @Schema(description = "ID del usuario destinatario", example = "1", required = true)
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

    /**
     * Dirección de correo electrónico del usuario destinatario.
     */
    @Schema(description = "Correo electrónico del usuario", example = "estudiante@universidad.edu", required = true)
    @NotBlank(message = "El email del usuario es obligatorio")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    private String userEmail;

    /**
     * Nombre completo del usuario destinatario.
     */
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García", required = true)
    @NotBlank(message = "El nombre del usuario es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String userName;

    /**
     * Rol del usuario en el sistema.
     */
    @Schema(description = "Rol del usuario", example = "ESTUDIANTE",
            allowableValues = {"ESTUDIANTE", "MAESTRO", "DIRECTOR_DE_AREA", "COORDINADOR"}, required = true)
    @NotBlank(message = "El rol del usuario es obligatorio")
    @Size(max = 50, message = "El rol no puede exceder 50 caracteres")
    private String userRole;

    /**
     * Tipo específico de notificación según el contexto.
     */
    @Schema(description = "Tipo de notificación", example = "STUDENT_SCHEDULE_ASSIGNMENT",
            allowableValues = {"STUDENT_SCHEDULE_ASSIGNMENT", "STUDENT_SCHEDULE_CHANGE", "STUDENT_CLASS_CANCELLATION",
                             "TEACHER_CLASS_SCHEDULED", "TEACHER_CLASS_MODIFIED", "TEACHER_CLASS_CANCELLED",
                             "TEACHER_AVAILABILITY_CHANGED", "DIRECTOR_SCHEDULE_CONFLICT", "DIRECTOR_AVAILABILITY_ISSUE",
                             "DIRECTOR_SYSTEM_INCIDENT", "COORDINATOR_GLOBAL_UPDATE", "COORDINATOR_SYSTEM_ALERT",
                             "COORDINATOR_CHANGE_CONFIRMATION", "COORDINATOR_MAINTENANCE_ALERT", "GENERAL_SYSTEM_NOTIFICATION"},
            required = true)
    @NotBlank(message = "El tipo de notificación es obligatorio")
    private String notificationType;

    /**
     * Título atractivo de la notificación.
     */
    @Schema(description = "Título de la notificación", example = "📚 Nuevo Horario Asignado", required = true)
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede exceder 255 caracteres")
    private String title;

    /**
     * Contenido detallado del mensaje de notificación.
     */
    @Schema(description = "Mensaje de la notificación", example = "Se ha asignado un nuevo horario para el semestre 2025-1.", required = true)
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
    private String message;

    /**
     * URL opcional para redireccionar al hacer clic.
     */
    @Schema(description = "URL de acción (opcional)", example = "/horarios")
    @Size(max = 500, message = "La URL de acción no puede exceder 500 caracteres")
    private String actionUrl;

    /**
     * Texto del botón de acción.
     */
    @Schema(description = "Texto del botón de acción", example = "Ver Horario")
    @Size(max = 100, message = "El texto de acción no puede exceder 100 caracteres")
    private String actionText;

    /**
     * Icono visual para la notificación (emoji o código).
     */
    @Schema(description = "Icono de la notificación", example = "📚", defaultValue = "🔔")
    @Size(max = 100, message = "El icono no puede exceder 100 caracteres")
    private String icon = "🔔";

    /**
     * Nivel de prioridad de la notificación.
     */
    @Schema(description = "Prioridad de la notificación", example = "MEDIUM",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}, defaultValue = "MEDIUM")
    private String priority = "MEDIUM";

    /**
     * Categoría temática de la notificación.
     */
    @Schema(description = "Categoría de la notificación", example = "SCHEDULE", defaultValue = "GENERAL",
            allowableValues = {"SCHEDULE", "CLASS", "SYSTEM", "GENERAL"})
    @Size(max = 50, message = "La categoría no puede exceder 50 caracteres")
    private String category = "GENERAL";

    /**
     * Timestamp de creación de la notificación.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Datos adicionales en formato clave-valor.
     */
    @Schema(description = "Datos adicionales")
    private Map<String, Object> metadata;

    /**
     * Enumeración de tipos de notificación.
     * Implementa patrón Enum para tipos seguros.
     */
    public enum NotificationTypeEnum {
        // Notificaciones para estudiantes
        STUDENT_SCHEDULE_ASSIGNMENT("Asignación de horario"),
        STUDENT_SCHEDULE_CHANGE("Cambio de horario"),
        STUDENT_CLASS_CANCELLATION("Cancelación de clase"),

        // Notificaciones para profesores
        TEACHER_CLASS_SCHEDULED("Clase programada"),
        TEACHER_CLASS_MODIFIED("Clase modificada"),
        TEACHER_CLASS_CANCELLED("Clase cancelada"),
        TEACHER_AVAILABILITY_CHANGED("Disponibilidad cambiada"),

        // Notificaciones para directores
        DIRECTOR_SCHEDULE_CONFLICT("Conflicto de horario"),
        DIRECTOR_AVAILABILITY_ISSUE("Problema de disponibilidad"),
        DIRECTOR_SYSTEM_INCIDENT("Incidente del sistema"),

        // Notificaciones para coordinadores
        COORDINATOR_GLOBAL_UPDATE("Actualización global"),
        COORDINATOR_SYSTEM_ALERT("Alerta del sistema"),
        COORDINATOR_CHANGE_CONFIRMATION("Confirmación de cambio"),
        COORDINATOR_MAINTENANCE_ALERT("Alerta de mantenimiento"),

        // Notificaciones generales
        GENERAL_SYSTEM_NOTIFICATION("Notificación general");

        private final String description;

        NotificationTypeEnum(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Método Factory para crear una notificación básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param userId ID del usuario
     * @param userEmail Email del usuario
     * @param userName Nombre del usuario
     * @param userRole Rol del usuario
     * @param notificationType Tipo de notificación
     * @param title Título
     * @param message Mensaje
     * @return InAppNotificationDTO configurado
     */
    public static InAppNotificationDTO create(Long userId, String userEmail, String userName,
                                            String userRole, String notificationType,
                                            String title, String message) {
        InAppNotificationDTO dto = new InAppNotificationDTO();
        dto.setUserId(userId);
        dto.setUserEmail(userEmail);
        dto.setUserName(userName);
        dto.setUserRole(userRole);
        dto.setNotificationType(notificationType);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear una notificación con acción.
     *
     * @param userId ID del usuario
     * @param userEmail Email del usuario
     * @param userName Nombre del usuario
     * @param userRole Rol del usuario
     * @param notificationType Tipo de notificación
     * @param title Título
     * @param message Mensaje
     * @param actionUrl URL de acción
     * @param actionText Texto de acción
     * @return InAppNotificationDTO con acción configurada
     */
    public static InAppNotificationDTO createWithAction(Long userId, String userEmail, String userName,
                                                      String userRole, String notificationType,
                                                      String title, String message, String actionUrl, String actionText) {
        InAppNotificationDTO dto = new InAppNotificationDTO();
        dto.setUserId(userId);
        dto.setUserEmail(userEmail);
        dto.setUserName(userName);
        dto.setUserRole(userRole);
        dto.setNotificationType(notificationType);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setActionUrl(actionUrl);
        dto.setActionText(actionText);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear una notificación de sistema.
     *
     * @param userId ID del usuario
     * @param title Título
     * @param message Mensaje
     * @return InAppNotificationDTO de sistema
     */
    public static InAppNotificationDTO createSystemNotification(Long userId, String title, String message) {
        InAppNotificationDTO dto = new InAppNotificationDTO();
        dto.setUserId(userId);
        dto.setNotificationType("GENERAL_SYSTEM_NOTIFICATION");
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setIcon("⚙️");
        dto.setPriority("MEDIUM");
        dto.setCategory("SYSTEM");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un InAppNotificationDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return InAppNotificationDTO con valores por defecto
     */
    public static InAppNotificationDTO empty() {
        InAppNotificationDTO dto = new InAppNotificationDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    // Getters y Setters
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

    @Override
    public LocalDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        super.setCreatedAt(createdAt);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Verifica si la notificación tiene alta prioridad.
     *
     * @return true si es de alta prioridad o crítica
     */
    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "CRITICAL".equals(priority);
    }

    /**
     * Verifica si la notificación tiene acción configurada.
     *
     * @return true si tiene URL y texto de acción
     */
    public boolean hasAction() {
        return actionUrl != null && !actionUrl.trim().isEmpty() &&
               actionText != null && !actionText.trim().isEmpty();
    }

    /**
     * Obtiene el tipo de notificación como enum tipado.
     *
     * @return NotificationTypeEnum correspondiente o null si no es válido
     */
    public NotificationTypeEnum getNotificationTypeAsEnum() {
        if (notificationType == null) {
            return null;
        }
        try {
            return NotificationTypeEnum.valueOf(notificationType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    public boolean isValid() {
        return userId != null && userId > 0 &&
               userEmail != null && !userEmail.trim().isEmpty() &&
               userName != null && !userName.trim().isEmpty() &&
               userRole != null && !userRole.trim().isEmpty() &&
               notificationType != null && !notificationType.trim().isEmpty() &&
               title != null && !title.trim().isEmpty() &&
               message != null && !message.trim().isEmpty();
    }

    /**
     * Obtiene una representación resumida de la notificación.
     * Formato: "[icon] [title] - [userName]"
     *
     * @return Representación resumida
     */
    public String getSummary() {
        return String.format("%s %s - %s",
                icon != null ? icon : "🔔",
                title != null ? title : "Sin título",
                userName != null ? userName : "Sin destinatario");
    }

    /**
     * Verifica si el rol del usuario es válido.
     *
     * @return true si el rol está en la lista de roles permitidos
     */
    public boolean hasValidUserRole() {
        List<String> validRoles = Arrays.asList("ESTUDIANTE", "MAESTRO", "DIRECTOR_DE_AREA", "COORDINADOR");
        return userRole != null && validRoles.contains(userRole.toUpperCase());
    }
}