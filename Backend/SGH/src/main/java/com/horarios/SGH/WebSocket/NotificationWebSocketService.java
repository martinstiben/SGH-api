package com.horarios.SGH.WebSocket;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio WebSocket para notificaciones en tiempo real
 * Permite enviar notificaciones instantáneas a React web y React Native móvil
 */
@Slf4j
@Service
public class NotificationWebSocketService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Mapa de sesiones activas por usuario (userId -> WebSocketSession)
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // Mapa de usuarios por sesión (sessionId -> userId)
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
    
    /**
     * Envía notificación a un usuario específico
     */
    public void sendNotificationToUser(String userId, InAppNotificationDTO notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("new_notification", notification);
                session.sendMessage(new TextMessage(message));
                log.debug("Notificación enviada en tiempo real a usuario {}: {}", userId, notification.getTitle());
            } catch (IOException e) {
                log.warn("Error enviando notificación WebSocket a usuario {}: {}", userId, e.getMessage());
                // Remover sesión cerrada
                removeUserSession(userId);
            }
        } else {
            log.debug("Usuario {} no tiene sesión WebSocket activa, notificación no enviada en tiempo real", userId);
        }
    }
    
    /**
     * Marca una notificación como leída para un usuario
     */
    public void sendReadStatusToUser(String userId, Long notificationId, boolean isRead) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                ReadStatusUpdate update = new ReadStatusUpdate(notificationId, isRead);
                String message = createWebSocketMessage("read_status_update", update);
                session.sendMessage(new TextMessage(message));
                log.debug("Estado de lectura enviado a usuario {}: notification {} = {}", userId, notificationId, isRead);
            } catch (IOException e) {
                log.warn("Error enviando estado de lectura WebSocket a usuario {}: {}", userId, e.getMessage());
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Marca todas las notificaciones como leídas para un usuario
     */
    public void sendBulkReadStatusToUser(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("bulk_read_update", Map.of("success", true));
                session.sendMessage(new TextMessage(message));
                log.debug("Actualización masiva de lectura enviada a usuario {}", userId);
            } catch (IOException e) {
                log.warn("Error enviando actualización masiva WebSocket a usuario {}: {}", userId, e.getMessage());
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Envía ping de conexión a un usuario
     */
    public void sendConnectionPing(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("ping", Map.of("timestamp", System.currentTimeMillis()));
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.warn("Error enviando ping WebSocket a usuario {}: {}", userId, e.getMessage());
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Registra una nueva sesión de usuario
     */
    public void registerUserSession(String userId, WebSocketSession session) {
        removeUserSession(userId); // Remover sesión anterior si existe
        userSessions.put(userId, session);
        sessionUsers.put(session.getId(), userId);
        log.info("Usuario {} conectado por WebSocket", userId);
        
        // Confirmar conexión exitosa
        try {
            String connectionMessage = createWebSocketMessage("connection_confirmed", 
                Map.of("userId", userId, "timestamp", System.currentTimeMillis()));
            session.sendMessage(new TextMessage(connectionMessage));
        } catch (IOException e) {
            log.warn("Error confirmando conexión WebSocket para usuario {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Remueve la sesión de un usuario
     */
    public void removeUserSession(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null) {
            sessionUsers.remove(session.getId());
            userSessions.remove(userId);
            try {
                session.close();
            } catch (Exception e) {
                log.warn("Error cerrando sesión WebSocket para usuario {}: {}", userId, e.getMessage());
            }
            log.info("Usuario {} desconectado de WebSocket", userId);
        }
    }
    
    /**
     * Remueve sesión por ID de sesión
     */
    public void removeSessionById(String sessionId) {
        String userId = sessionUsers.get(sessionId);
        if (userId != null) {
            removeUserSession(userId);
        } else {
            sessionUsers.remove(sessionId);
        }
    }
    
    /**
     * Obtiene el número de usuarios conectados
     */
    public int getConnectedUsersCount() {
        return (int) userSessions.values().stream().filter(WebSocketSession::isOpen).count();
    }
    
    /**
     * Verifica si un usuario está conectado
     */
    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * Crea mensaje WebSocket estructurado
     */
    private String createWebSocketMessage(String type, Object data) {
        try {
            WebSocketMessage message = new WebSocketMessage(type, data, System.currentTimeMillis());
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.warn("Error serializando mensaje WebSocket: {}", e.getMessage());
            return "{\"type\":\"error\",\"data\":{\"message\":\"Error serializando mensaje\"}}";
        }
    }
    
    /**
     * Clases para mensajes WebSocket
     */
    public static class WebSocketMessage {
        private String type;
        private Object data;
        private long timestamp;
        
        public WebSocketMessage() {}
        
        public WebSocketMessage(String type, Object data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        // Getters y setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    public static class ReadStatusUpdate {
        private Long notificationId;
        private boolean isRead;
        
        public ReadStatusUpdate() {}
        
        public ReadStatusUpdate(Long notificationId, boolean isRead) {
            this.notificationId = notificationId;
            this.isRead = isRead;
        }
        
        // Getters y setters
        public Long getNotificationId() { return notificationId; }
        public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
    }
}