package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define las categorías de notificaciones en el sistema SGH.
 * Cada categoría agrupa notificaciones relacionadas por tema o funcionalidad,
 * facilitando la organización y filtrado de las notificaciones por parte de los usuarios.
 *
 * Esta enumeración ayuda a los usuarios a identificar rápidamente el tipo de
 * información que contiene cada notificación.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Schema(description = "Categorías de notificaciones del sistema SGH")
public enum NotificationCategory {
    
    /**
     * Notificaciones relacionadas con horarios académicos
     */
    SCHEDULE("Horario"),
    
    /**
     * Notificaciones relacionadas con clases específicas
     */
    CLASS("Clase"),
    
    /**
     * Notificaciones del sistema
     */
    SYSTEM("Sistema"),
    
    /**
     * Notificaciones generales
     */
    GENERAL("General");
    
    private final String displayName;
    
    NotificationCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Obtiene el icono asociado a la categoría
     */
    public String getIcon() {
        switch (this) {
            case SCHEDULE: return "📅";
            case CLASS: return "🏫";
            case SYSTEM: return "💻";
            case GENERAL: return "📢";
            default: return "🔔";
        }
    }
    
    /**
     * Obtiene el color asociado a la categoría
     */
    public String getColor() {
        switch (this) {
            case SCHEDULE: return "#2196F3"; // Azul
            case CLASS: return "#4CAF50";   // Verde
            case SYSTEM: return "#F44336";  // Rojo
            case GENERAL: return "#FF9800"; // Naranja
            default: return "#9E9E9E";
        }
    }
}