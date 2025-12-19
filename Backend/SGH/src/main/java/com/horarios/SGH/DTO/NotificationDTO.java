package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para el envío de notificaciones por correo electrónico del sistema SGH.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones de negocio y métodos de utilidad para
 * construcción y validación de notificaciones por email.
 *
 * Proporciona métodos Factory para crear diferentes tipos de notificaciones
 * y validaciones específicas para el envío de correos electrónicos.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar notificaciones
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Implementado a través de AbstractDTO
 * - Factory Method: Para creación de instancias
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos para enviar una notificación por correo electrónico")
public class NotificationDTO extends AbstractDTO {

    @Schema(description = "Asunto del correo electrónico", example = "¡Bienvenido al Sistema SGH!", required = true)
    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede exceder 255 caracteres")
    private String subject;

    @Schema(description = "Contenido del mensaje", example = "Su cuenta ha sido creada exitosamente.", required = true)
    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 5000, message = "El contenido no puede exceder 5000 caracteres")
    private String content;

    @Schema(description = "Correo electrónico del destinatario", example = "estudiante@universidad.edu", required = true)
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email de destino es obligatorio")
    private String recipientEmail;

    @Schema(description = "Nombre completo del destinatario", example = "Juan Pérez García", required = true)
    @NotBlank(message = "El nombre completo del destinatario es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String recipientName;

    @Schema(description = "Rol del destinatario", example = "ESTUDIANTE", allowableValues = {"ESTUDIANTE", "MAESTRO", "DIRECTOR_DE_AREA", "COORDINADOR"})
    private String recipientRole = "ESTUDIANTE";

    @Schema(description = "Tipo de notificación", example = "STUDENT_SCHEDULE_ASSIGNMENT",
            allowableValues = {"STUDENT_SCHEDULE_ASSIGNMENT", "STUDENT_SCHEDULE_CHANGE", "STUDENT_CLASS_CANCELLATION",
                             "TEACHER_CLASS_SCHEDULED", "TEACHER_CLASS_MODIFIED", "TEACHER_CLASS_CANCELLED",
                             "TEACHER_AVAILABILITY_CHANGED", "DIRECTOR_SCHEDULE_CONFLICT", "DIRECTOR_AVAILABILITY_ISSUE",
                             "DIRECTOR_SYSTEM_INCIDENT", "COORDINATOR_GLOBAL_UPDATE", "COORDINATOR_SYSTEM_ALERT",
                             "COORDINATOR_CHANGE_CONFIRMATION", "COORDINATOR_MAINTENANCE_ALERT", "GENERAL_SYSTEM_NOTIFICATION"})
    private String notificationType = "GENERAL_SYSTEM_NOTIFICATION";

    @Schema(description = "Nombre del remitente", example = "Sistema SGH", defaultValue = "Sistema SGH")
    private String senderName = "Sistema SGH";

    @Schema(description = "Indica si el contenido es HTML", example = "true", defaultValue = "false")
    private Boolean isHtml = false;

    /**
     * Constructor por defecto.
     */
    public NotificationDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param recipientEmail Email del destinatario
     * @param recipientName Nombre del destinatario
     * @param subject Asunto del correo
     * @param content Contenido del mensaje
     */
    public NotificationDTO(String recipientEmail, String recipientName, String subject, String content) {
        super();
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.content = content;
        this.recipientRole = "ESTUDIANTE";
        this.notificationType = "GENERAL_SYSTEM_NOTIFICATION";
        this.senderName = "Sistema SGH";
        this.isHtml = false;
        this.createdAt = LocalDateTime.now();
    }

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
     * Método Factory para crear una notificación básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param recipientEmail Email del destinatario
     * @param recipientName Nombre del destinatario
     * @param subject Asunto del correo
     * @param content Contenido del mensaje
     * @return NotificationDTO configurado
     */
    public static NotificationDTO create(String recipientEmail, String recipientName,
                                       String subject, String content) {
        NotificationDTO dto = new NotificationDTO();
        dto.setRecipientEmail(recipientEmail);
        dto.setRecipientName(recipientName);
        dto.setSubject(subject);
        dto.setContent(content);
        dto.setRecipientRole("ESTUDIANTE");
        dto.setNotificationType("GENERAL_SYSTEM_NOTIFICATION");
        dto.setSenderName("Sistema SGH");
        dto.setIsHtml(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear una notificación con HTML.
     *
     * @param recipientEmail Email del destinatario
     * @param recipientName Nombre del destinatario
     * @param subject Asunto del correo
     * @param htmlContent Contenido HTML del mensaje
     * @return NotificationDTO con contenido HTML
     */
    public static NotificationDTO createHtml(String recipientEmail, String recipientName,
                                           String subject, String htmlContent) {
        NotificationDTO dto = create(recipientEmail, recipientName, subject, htmlContent);
        dto.setIsHtml(true);
        return dto;
    }

    /**
     * Método Factory para crear un NotificationDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return NotificationDTO con valores por defecto
     */
    public static NotificationDTO empty() {
        NotificationDTO dto = new NotificationDTO();
        dto.setIsHtml(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si la notificación tiene contenido HTML.
     *
     * @return true si el contenido es HTML
     */
    public boolean isHtmlContent() {
        return Boolean.TRUE.equals(isHtml);
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return recipientEmail != null && !recipientEmail.trim().isEmpty() &&
               recipientName != null && !recipientName.trim().isEmpty() &&
               subject != null && !subject.trim().isEmpty() &&
               content != null && !content.trim().isEmpty();
    }

    /**
     * Obtiene una representación resumida de la notificación.
     * Formato: "[subject] -> [recipientEmail]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s -> %s",
                subject != null ? subject : "Sin asunto",
                recipientEmail != null ? recipientEmail : "Sin destinatario");
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

    // Getters y Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Boolean getIsHtml() {
        return isHtml;
    }

    public void setIsHtml(Boolean isHtml) {
        this.isHtml = isHtml;
    }
}
