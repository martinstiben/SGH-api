package com.horarios.SGH.Model;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de notificaciones disponibles en el sistema SGH
 * Cada tipo corresponde a eventos específicos del sistema de gestión de horarios
 */
public enum NotificationType {
    
    // Estudiantes - notificaciones sobre asignaciones o cambios en sus horarios
    STUDENT_SCHEDULE_ASSIGNMENT("Asignación de Horario"),
    
    STUDENT_SCHEDULE_CHANGE("Cambio de Horario"),
    
    STUDENT_CLASS_CANCELLATION("Cancelación de Clase"),
    
    // Maestros - notificaciones sobre clases programadas, modificaciones o cancelaciones
    TEACHER_CLASS_SCHEDULED("Clase Programada"),
    
    TEACHER_CLASS_MODIFIED("Modificación de Clase"),
    
    TEACHER_CLASS_CANCELLED("Clase Cancelada"),
    
    TEACHER_AVAILABILITY_CHANGED("Cambio de Disponibilidad"),
    
    // Directores de Área - alertas sobre disponibilidad de horarios, conflictos o incidencias
    DIRECTOR_AVAILABILITY_ISSUE("Problema de Disponibilidad"),

    DIRECTOR_SYSTEM_INCIDENT("Incidencia del Sistema"),

    // Coordinadores - notificaciones generales, actualizaciones globales, confirmaciones
    COORDINATOR_GLOBAL_UPDATE("Actualización Global"),

    COORDINATOR_SYSTEM_ALERT("Alerta del Sistema"),

    COORDINATOR_CHANGE_CONFIRMATION("Confirmación de Cambio"),
    
    // Notificaciones generales para todos los roles
    GENERAL_SYSTEM_NOTIFICATION("Notificación General");
    
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
            case STUDENT_SCHEDULE_ASSIGNMENT:
            case STUDENT_SCHEDULE_CHANGE:
            case STUDENT_CLASS_CANCELLATION:
                return new String[]{"ESTUDIANTE"};

            case TEACHER_CLASS_SCHEDULED:
            case TEACHER_CLASS_MODIFIED:
            case TEACHER_CLASS_CANCELLED:
            case TEACHER_AVAILABILITY_CHANGED:
                return new String[]{"MAESTRO"};

            case DIRECTOR_AVAILABILITY_ISSUE:
            case DIRECTOR_SYSTEM_INCIDENT:
                return new String[]{"DIRECTOR_DE_AREA"};

            case COORDINATOR_GLOBAL_UPDATE:
            case COORDINATOR_SYSTEM_ALERT:
            case COORDINATOR_CHANGE_CONFIRMATION:
                return new String[]{"COORDINADOR"};

            case GENERAL_SYSTEM_NOTIFICATION:
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
}