package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el envío de notificaciones In-App (solo campos de request)
 * Utilizado para enviar notificaciones en tiempo real a React web y React Native móvil
 */
@Data
@Schema(description = "Datos para enviar una notificación In-App")
public class InAppNotificationDTO {

    @Schema(description = "ID del usuario destinatario", example = "1", required = true)
    @NotNull(message = "El ID de usuario es obligatorio")
    private Integer userId;

    @Schema(description = "Correo electrónico del usuario", example = "estudiante@universidad.edu", required = true)
    @NotBlank(message = "El email del usuario es obligatorio")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    private String userEmail;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García", required = true)
    @NotBlank(message = "El nombre del usuario es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String userName;

    @Schema(description = "Rol del usuario", example = "ESTUDIANTE",
            allowableValues = {"ESTUDIANTE", "MAESTRO", "DIRECTOR_DE_AREA", "COORDINADOR"}, required = true)
    @NotBlank(message = "El rol del usuario es obligatorio")
    @Size(max = 50, message = "El rol no puede exceder 50 caracteres")
    private String userRole;

    @Schema(description = "Tipo de notificación", example = "STUDENT_SCHEDULE_ASSIGNMENT",
            allowableValues = {"STUDENT_SCHEDULE_ASSIGNMENT", "STUDENT_SCHEDULE_CHANGE", "STUDENT_CLASS_CANCELLATION",
                             "TEACHER_CLASS_SCHEDULED", "TEACHER_CLASS_MODIFIED", "TEACHER_CLASS_CANCELLED",
                             "TEACHER_AVAILABILITY_CHANGED", "DIRECTOR_SCHEDULE_CONFLICT", "DIRECTOR_AVAILABILITY_ISSUE",
                             "DIRECTOR_SYSTEM_INCIDENT", "COORDINATOR_GLOBAL_UPDATE", "COORDINATOR_SYSTEM_ALERT",
                             "COORDINATOR_CHANGE_CONFIRMATION", "COORDINATOR_MAINTENANCE_ALERT", "GENERAL_SYSTEM_NOTIFICATION"},
            required = true)
    @NotBlank(message = "El tipo de notificación es obligatorio")
    private String notificationType;

    @Schema(description = "Título de la notificación", example = "📚 Nuevo Horario Asignado", required = true)
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede exceder 255 caracteres")
    private String title;

    @Schema(description = "Mensaje de la notificación", example = "Se ha asignado un nuevo horario para el semestre 2025-1.", required = true)
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
    private String message;

    @Schema(description = "URL de acción (opcional)", example = "/horarios")
    @Size(max = 500, message = "La URL de acción no puede exceder 500 caracteres")
    private String actionUrl;

    @Schema(description = "Texto del botón de acción", example = "Ver Horario")
    @Size(max = 100, message = "El texto de acción no puede exceder 100 caracteres")
    private String actionText;

    @Schema(description = "Icono de la notificación", example = "📚", defaultValue = "🔔")
    @Size(max = 100, message = "El icono no puede exceder 100 caracteres")
    private String icon = "🔔";

    @Schema(description = "Prioridad de la notificación", example = "MEDIUM",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}, defaultValue = "MEDIUM")
    private String priority = "MEDIUM";

    @Schema(description = "Categoría de la notificación", example = "schedule", defaultValue = "general")
    @Size(max = 50, message = "La categoría no puede exceder 50 caracteres")
    private String category = "general";
}