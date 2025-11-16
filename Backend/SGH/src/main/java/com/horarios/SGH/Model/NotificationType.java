package com.horarios.SGH.Model;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de notificaciones esenciales para el Sistema de Gestión de Horarios (SGH)
 */
public enum NotificationType {

    // Notificaciones principales automatizadas
    TEACHER_SCHEDULE_ASSIGNED("Nueva Asignación de Clase"),
    SCHEDULE_ASSIGNED("Horario Académico Asignado"),
    SYSTEM_ALERT("Alerta del Sistema"),
    SYSTEM_NOTIFICATION("Notificación del Sistema");

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
