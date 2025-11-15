package com.horarios.SGH.Model;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de notificaciones específicas para el Sistema de Gestión de Horarios (SGH)
 */
public enum NotificationType {

    // Notificaciones para Estudiantes
    SCHEDULE_ASSIGNED("Horario Asignado"),
    SCHEDULE_CHANGED("Horario Modificado"),
    SCHEDULE_CONFLICT("Conflicto de Horario"),
    WELCOME_STUDENT("Bienvenida Estudiante"),

    // Notificaciones para Maestros
    TEACHER_SCHEDULE_ASSIGNED("Asignación de Horario Docente"),
    TEACHER_CONFLICT_DETECTED("Conflicto Detectado"),
    SCHEDULE_UPDATE_REQUIRED("Actualización de Horario Requerida"),
    WELCOME_TEACHER("Bienvenida Docente"),

    // Notificaciones para Directores
    CRITICAL_SCHEDULE_CONFLICTS("Conflictos Críticos de Horario"),
    SCHEDULE_OVERVIEW_REPORT("Reporte General de Horarios"),
    DEPARTMENT_ALERT("Alerta Departamental"),
    WELCOME_DIRECTOR("Bienvenida Director"),

    // Notificaciones para Coordinadores
    SYSTEM_MAINTENANCE("Mantenimiento del Sistema"),
    BULK_SCHEDULE_UPDATE("Actualización Masiva de Horarios"),
    SYSTEM_NOTIFICATION("Notificación del Sistema"),
    WELCOME_COORDINATOR("Bienvenida Coordinador");

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
            // Estudiantes
            case SCHEDULE_ASSIGNED:
            case SCHEDULE_CHANGED:
            case SCHEDULE_CONFLICT:
            case WELCOME_STUDENT:
                return new String[]{"ESTUDIANTE"};

            // Maestros
            case TEACHER_SCHEDULE_ASSIGNED:
            case TEACHER_CONFLICT_DETECTED:
            case SCHEDULE_UPDATE_REQUIRED:
            case WELCOME_TEACHER:
                return new String[]{"MAESTRO"};

            // Directores
            case CRITICAL_SCHEDULE_CONFLICTS:
            case SCHEDULE_OVERVIEW_REPORT:
            case DEPARTMENT_ALERT:
            case WELCOME_DIRECTOR:
                return new String[]{"DIRECTOR_DE_AREA"};

            // Coordinadores
            case SYSTEM_MAINTENANCE:
            case BULK_SCHEDULE_UPDATE:
            case WELCOME_COORDINATOR:
                return new String[]{"COORDINADOR"};

            // Sistema (todos los roles)
            case SYSTEM_NOTIFICATION:
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
            case SCHEDULE_ASSIGNED:
            case TEACHER_SCHEDULE_ASSIGNED:
                return "📚";
            case SCHEDULE_CHANGED:
            case TEACHER_CONFLICT_DETECTED:
                return "⚠️";
            case SCHEDULE_CONFLICT:
            case CRITICAL_SCHEDULE_CONFLICTS:
                return "🚨";
            case WELCOME_STUDENT:
            case WELCOME_TEACHER:
            case WELCOME_DIRECTOR:
            case WELCOME_COORDINATOR:
                return "🎉";
            case SYSTEM_MAINTENANCE:
            case SYSTEM_NOTIFICATION:
                return "⚙️";
            case SCHEDULE_OVERVIEW_REPORT:
                return "📊";
            case DEPARTMENT_ALERT:
                return "📢";
            case BULK_SCHEDULE_UPDATE:
                return "🔄";
            case SCHEDULE_UPDATE_REQUIRED:
                return "🔧";
            default:
                return "📧";
        }
    }

    /**
     * Obtiene el color correspondiente al tipo de notificación
     */
    public String getColor() {
        switch (this) {
            case SCHEDULE_ASSIGNED:
            case TEACHER_SCHEDULE_ASSIGNED:
                return "#4CAF50";
            case SCHEDULE_CHANGED:
                return "#2196F3";
            case SCHEDULE_CONFLICT:
            case TEACHER_CONFLICT_DETECTED:
                return "#FF9800";
            case CRITICAL_SCHEDULE_CONFLICTS:
                return "#F44336";
            case WELCOME_STUDENT:
            case WELCOME_TEACHER:
            case WELCOME_DIRECTOR:
            case WELCOME_COORDINATOR:
                return "#9C27B0";
            case SYSTEM_MAINTENANCE:
            case SYSTEM_NOTIFICATION:
                return "#607D8B";
            case SCHEDULE_OVERVIEW_REPORT:
                return "#3F51B5";
            case DEPARTMENT_ALERT:
                return "#FF5722";
            case BULK_SCHEDULE_UPDATE:
                return "#795548";
            case SCHEDULE_UPDATE_REQUIRED:
                return "#E91E63";
            default:
                return "#9E9E9E";
        }
    }
}
