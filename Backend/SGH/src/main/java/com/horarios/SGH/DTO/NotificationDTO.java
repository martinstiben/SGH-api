package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el envío de notificaciones por correo electrónico
 * Solo incluye los campos necesarios para el request
 */
@Data
@Schema(description = "Datos para enviar una notificación por correo electrónico")
public class NotificationDTO {

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
}