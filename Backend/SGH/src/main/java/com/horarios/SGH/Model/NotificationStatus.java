package com.horarios.SGH.Model;

/**
 * Estados posibles de las notificaciones en el sistema SGH
 * Define el flujo de vida de una notificación desde su creación hasta su resolución
 */
public enum NotificationStatus {
    
    /**
     * Notificación creada y pendiente de envío
     */
    PENDING("Pendiente"),
    
    /**
     * Notificación enviada exitosamente
     */
    SENT("Enviada"),
    
    /**
     * Envío fallido, pero se puede reintentar
     */
    RETRY("Reintentando"),
    
    /**
     * Envío fallido después de todos los intentos
     */
    FAILED("Fallida"),
    
    /**
     * Envío cancelado por el usuario o sistema
     */
    CANCELLED("Cancelada"),
    
    /**
     * Notificación en proceso de envío
     */
    SENDING("Enviando");
    
    private final String displayName;
    
    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Verifica si el estado indica que la notificación está activa (pendiente o en proceso)
     */
    public boolean isActive() {
        return this == PENDING || this == SENDING || this == RETRY;
    }
    
    /**
     * Verifica si el estado indica que la notificación fue resuelta
     */
    public boolean isResolved() {
        return this == SENT || this == FAILED || this == CANCELLED;
    }
    
    /**
     * Verifica si el estado indica un fallo
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * Obtiene el color asociado al estado para interfaces gráficas
     */
    public String getColor() {
        switch (this) {
            case PENDING: return "orange";
            case SENT: return "green";
            case RETRY: return "yellow";
            case FAILED: return "red";
            case CANCELLED: return "gray";
            case SENDING: return "blue";
            default: return "black";
        }
    }
}