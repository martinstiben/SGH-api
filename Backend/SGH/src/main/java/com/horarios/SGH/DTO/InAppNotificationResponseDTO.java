package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta para notificaciones In-App
 * Incluye todos los campos de respuesta que se muestran en Swagger
 */
@Data
@Schema(description = "Respuesta de notificación In-App")
public class InAppNotificationResponseDTO {

    @Schema(description = "ID único de la notificación", example = "123")
    private Long notificationId;

    @Schema(description = "ID del usuario destinatario", example = "1")
    private Integer userId;

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
    private boolean isRead;

    @Schema(description = "Indica si la notificación está archivada", example = "false")
    private boolean isArchived;

    @Schema(description = "Fecha de expiración (opcional)")
    private LocalDateTime expiresAt;

    @Schema(description = "Datos adicionales en formato JSON")
    private Map<String, Object> metadata;

    @Schema(description = "Fecha de creación", example = "2025-11-12T21:15:00")
    private LocalDateTime createdAt;

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
    private boolean isRecent;

    @Schema(description = "Indica si la notificación está activa", example = "true")
    private boolean isActive;

    @Schema(description = "Indica si requiere atención inmediata", example = "false")
    private boolean requiresImmediateAttention;
}