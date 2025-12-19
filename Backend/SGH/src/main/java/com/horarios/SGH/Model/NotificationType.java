package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeración que define los tipos de notificaciones disponibles en el sistema SGH.
 * Cada tipo está asociado a roles específicos y define el contenido y destinatarios
 * de las notificaciones del sistema.
 *
 * Esta enumeración es fundamental para el sistema de notificaciones, permitiendo
 * enviar mensajes contextualizados según el rol del usuario y el tipo de evento.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Schema(description = "Tipos de notificaciones del sistema SGH")
public enum NotificationType {

    // Notificaciones principales automatizadas
    TEACHER_SCHEDULE_ASSIGNED("Nueva Asignación de Clase"),
    SCHEDULE_ASSIGNED("Horario Académico Asignado"),
    SYSTEM_ALERT("Alerta del Sistema"),
    SYSTEM_NOTIFICATION("Notificación del Sistema"),

    // Coordinadores - notificaciones generales, actualizaciones globales, confirmaciones
    COORDINATOR_GLOBAL_UPDATE("Actualización Global"),

    COORDINATOR_SYSTEM_ALERT("Alerta del Sistema"),

    COORDINATOR_CHANGE_CONFIRMATION("Confirmación de Cambio"),

    COORDINATOR_USER_REGISTRATION_PENDING("Usuario Pendiente de Aprobación"),

    COORDINATOR_USER_APPROVED("Usuario Aprobado"),

    COORDINATOR_USER_REJECTED("Usuario Rechazado"),

    // Notificaciones generales para todos los roles
    GENERAL_SYSTEM_NOTIFICATION("Notificación General"),

    // Notificaciones de registro y aprobación de usuarios
    USER_REGISTRATION_APPROVED("Registro Aprobado"),

    USER_REGISTRATION_REJECTED("Registro Rechazado");
    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determina qué roles pueden recibir este tipo de notificación
     */
    public String[] getAllowedRoles() {
        switch (this) {
            // Profesores - notificaciones de asignación de clases
            case TEACHER_SCHEDULE_ASSIGNED:
                return new String[]{"MAESTRO"};

            // Estudiantes - notificaciones de horarios
            case SCHEDULE_ASSIGNED:
                return new String[]{"ESTUDIANTE"};

            // Directores - alertas críticas del sistema
            case SYSTEM_ALERT:
                return new String[]{"DIRECTOR_DE_AREA"};

            // Sistema - notificaciones generales para coordinadores
            case SYSTEM_NOTIFICATION:
                return new String[]{"COORDINADOR"};

            case COORDINATOR_GLOBAL_UPDATE:
            case COORDINATOR_SYSTEM_ALERT:
            case COORDINATOR_CHANGE_CONFIRMATION:
            case COORDINATOR_USER_REGISTRATION_PENDING:
            case COORDINATOR_USER_APPROVED:
            case COORDINATOR_USER_REJECTED:
                return new String[]{"COORDINADOR"};

            case GENERAL_SYSTEM_NOTIFICATION:
                return new String[]{"COORDINADOR", "MAESTRO", "ESTUDIANTE", "DIRECTOR_DE_AREA"};

            case USER_REGISTRATION_APPROVED:
            case USER_REGISTRATION_REJECTED:
                return new String[]{"COORDINADOR", "MAESTRO", "ESTUDIANTE", "DIRECTOR_DE_AREA"};
            default:
                return new String[]{};
        }
    }

    /**
     * Obtiene el tipo de notificación basado en el rol
     */
    public static NotificationType[] getTypesForRole(String role) {
        List<NotificationType> types = Arrays.asList(NotificationType.values());
        return types.stream()
            .filter(type -> {
                String[] allowedRoles = type.getAllowedRoles();
                for (String allowedRole : allowedRoles) {
                    if (allowedRole.equals(role)) {
                        return true;
                    }
                }
                return false;
            })
            .toArray(NotificationType[]::new);
    }

    /**
     * Obtiene el icono correspondiente al tipo de notificación
     */
    public String getIcon() {
        switch (this) {
            case TEACHER_SCHEDULE_ASSIGNED:
                return "👨‍🏫";
            case SCHEDULE_ASSIGNED:
                return "📚";
            case SYSTEM_ALERT:
                return "🚨";
            case SYSTEM_NOTIFICATION:
                return "📢";
            default:
                return "📧";
        }
    }

    /**
     * Obtiene el color correspondiente al tipo de notificación
     */
    public String getColor() {
        switch (this) {
            case TEACHER_SCHEDULE_ASSIGNED:
                return "#2196F3"; // Azul para profesores
            case SCHEDULE_ASSIGNED:
                return "#4CAF50"; // Verde para estudiantes
            case SYSTEM_ALERT:
                return "#F44336"; // Rojo para alertas directores
            case SYSTEM_NOTIFICATION:
                return "#FF9800"; // Naranja para coordinadores
            default:
                return "#9E9E9E";
        }
    }
}
