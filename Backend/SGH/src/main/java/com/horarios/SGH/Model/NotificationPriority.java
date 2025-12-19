package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define los niveles de prioridad para las notificaciones In-App del sistema SGH.
 * Cada nivel determina la urgencia, importancia y presentación visual de las notificaciones.
 *
 * Esta enumeración es fundamental para el sistema de notificaciones, permitiendo
 * clasificar las alertas según su criticidad y requerir diferentes niveles de atención del usuario.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Schema(description = "Niveles de prioridad para notificaciones In-App")
public enum NotificationPriority {
    
    /**
     * Prioridad baja - notificaciones informativas
     */
    LOW("Baja"),
    
    /**
     * Prioridad media - notificaciones estándar
     */
    MEDIUM("Media"),
    
    /**
     * Prioridad alta - notificaciones importantes que requieren atención
     */
    HIGH("Alta"),
    
    /**
     * Prioridad crítica - notificaciones urgentes que requieren acción inmediata
     */
    CRITICAL("Crítica");
    
    private final String displayName;
    
    NotificationPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Obtiene el color asociado a la prioridad para interfaces gráficas
     */
    public String getColor() {
        switch (this) {
            case LOW: return "#6c757d";       // Gris
            case MEDIUM: return "#17a2b8";    // Azul info
            case HIGH: return "#ffc107";      // Amarillo warning
            case CRITICAL: return "#dc3545";  // Rojo danger
            default: return "#6c757d";
        }
    }
    
    /**
     * Obtiene el icono asociado a la prioridad
     */
    public String getIcon() {
        switch (this) {
            case LOW: return "ℹ️";
            case MEDIUM: return "🔔";
            case HIGH: return "⚠️";
            case CRITICAL: return "🚨";
            default: return "🔔";
        }
    }
    
    /**
     * Verifica si la prioridad requiere atención inmediata
     */
    public boolean requiresImmediateAttention() {
        return this == CRITICAL || this == HIGH;
    }
    
    /**
     * Orden de prioridad (menor número = menor prioridad)
     */
    public int getOrder() {
        switch (this) {
            case LOW: return 1;
            case MEDIUM: return 2;
            case HIGH: return 3;
            case CRITICAL: return 4;
            default: return 1;
        }
    }
}