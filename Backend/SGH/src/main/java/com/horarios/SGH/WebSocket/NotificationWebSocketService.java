package com.horarios.SGH.WebSocket;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servicio WebSocket para notificaciones en tiempo real del sistema SGH.
 * Gestiona conexiones WebSocket bidireccionales para envío instantáneo de notificaciones
 * a clientes React web y React Native móvil.
 * 
 * Este servicio implementa el patrón Observer para notificaciones en tiempo real,
 * manteniendo un mapa de sesiones activas por usuario y proporcionando métodos
 * para envío de mensajes estructurados.
 * 
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de gestionar conexiones WebSocket y notificaciones
 * - OCP: Abierto para extensión mediante nuevas estrategias de mensaje
 * - LSP: Implementaciones sustituibles
 * - ISP: Interfaces específicas para diferentes tipos de mensaje
 * - DIP: Depende de abstracciones (ObjectMapper, WebSocketSession)
 * 
 * Funcionalidades principales:
 * - Gestión de sesiones WebSocket por usuario
 * - Envío de notificaciones en tiempo real
 * - Actualización de estados de lectura
 * - Ping de conexión y heartbeats
 * - Serialización automática de mensajes JSON
 * - Limpieza automática de sesiones cerradas
 * 
 * @author Sistema SGH
 * @version 1.0
 * @since 1.0
 */
@Service
public class NotificationWebSocketService {

    /**
     * Logger para registro de eventos del servicio WebSocket.
     */
    private static final Logger logger = Logger.getLogger(NotificationWebSocketService.class.getName());
    
    /**
     * Logger estático para compatibilidad con código existente.
     */
    private static final Logger log = logger;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Mapa de sesiones activas por usuario (userId -> WebSocketSession).
     * Utiliza ConcurrentHashMap para thread-safety en entornos concurrentes.
     */
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    /**
     * Mapa de usuarios por sesión (sessionId -> userId).
     * Permite lookup inverso para gestión eficiente de desconexiones.
     */
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
    
    /**
     * Envía notificación a un usuario específico a través de su sesión WebSocket activa.
     * Serializa la notificación en formato JSON y la envía como mensaje de texto.
     * Si la sesión no está activa, registra el evento pero no envía la notificación.
     *
     * @param userId Identificador único del usuario destinatario
     * @param notification DTO con los datos de la notificación a enviar
     * @throws IllegalArgumentException si userId o notification son null
     */
    public void sendNotificationToUser(String userId, InAppNotificationDTO notification) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El userId no puede ser null o vacío");
        }
        if (notification == null) {
            throw new IllegalArgumentException("La notificación no puede ser null");
        }

        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("new_notification", notification);
                session.sendMessage(new TextMessage(message));
                logger.log(Level.FINE, "Notificación enviada en tiempo real a usuario {0}: {1}", 
                          new Object[]{userId, notification.getTitle()});
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error enviando notificación WebSocket a usuario " + userId + ": " + e.getMessage(), e);
                // Remover sesión cerrada
                removeUserSession(userId);
            }
        } else {
            logger.log(Level.FINE, "Usuario {0} no tiene sesión WebSocket activa, notificación no enviada en tiempo real", userId);
        }
    }
    
    /**
     * Envía actualización del estado de lectura de una notificación específica a un usuario.
     * Utiliza el patrón Observer para notificar cambios de estado en tiempo real.
     *
     * @param userId Identificador único del usuario destinatario
     * @param notificationId ID de la notificación cuyo estado cambió
     * @param isRead Nuevo estado de lectura (true = leída, false = no leída)
     */
    public void sendReadStatusToUser(String userId, Long notificationId, boolean isRead) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                ReadStatusUpdate update = new ReadStatusUpdate(notificationId, isRead);
                String message = createWebSocketMessage("read_status_update", update);
                session.sendMessage(new TextMessage(message));
                logger.log(Level.FINE, "Estado de lectura enviado a usuario {0}: notification {1} = {2}", 
                          new Object[]{userId, notificationId, isRead});
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error enviando estado de lectura WebSocket a usuario " + userId + ": " + e.getMessage(), e);
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Envía confirmación de actualización masiva del estado de lectura para todas las notificaciones de un usuario.
     * Utilizado cuando el usuario marca todas las notificaciones como leídas desde la interfaz.
     *
     * @param userId Identificador único del usuario destinatario
     */
    public void sendBulkReadStatusToUser(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("bulk_read_update", Map.of("success", true));
                session.sendMessage(new TextMessage(message));
                logger.log(Level.FINE, "Actualización masiva de lectura enviada a usuario {0}", userId);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error enviando actualización masiva WebSocket a usuario " + userId + ": " + e.getMessage(), e);
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Envía un ping de conexión (heartbeat) a un usuario para verificar que la sesión WebSocket está activa.
     * Utilizado para detectar desconexiones y mantener la conexión viva en clientes que no envían mensajes.
     *
     * @param userId Identificador único del usuario destinatario
     */
    public void sendConnectionPing(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = createWebSocketMessage("ping", Map.of("timestamp", System.currentTimeMillis()));
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error enviando ping WebSocket a usuario " + userId + ": " + e.getMessage(), e);
                removeUserSession(userId);
            }
        }
    }
    
    /**
     * Registra una nueva sesión WebSocket para un usuario específico.
     * Remueve automáticamente cualquier sesión anterior del mismo usuario para evitar sesiones duplicadas.
     * Envía confirmación de conexión exitosa al cliente con timestamp para sincronización.
     *
     * @param userId Identificador único del usuario que se conecta
     * @param session Sesión WebSocket recién establecida
     * @throws IllegalArgumentException si userId o session son null
     */
    public void registerUserSession(String userId, WebSocketSession session) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El userId no puede ser null o vacío");
        }
        if (session == null) {
            throw new IllegalArgumentException("La sesión WebSocket no puede ser null");
        }
        
        removeUserSession(userId); // Remover sesión anterior si existe
        userSessions.put(userId, session);
        sessionUsers.put(session.getId(), userId);
        logger.log(Level.INFO, "Usuario {0} conectado por WebSocket", userId);
        
        // Confirmar conexión exitosa
        try {
            String connectionMessage = createWebSocketMessage("connection_confirmed", 
                Map.of("userId", userId, "timestamp", System.currentTimeMillis()));
            session.sendMessage(new TextMessage(connectionMessage));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error confirmando conexión WebSocket para usuario " + userId + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Remueve la sesión WebSocket de un usuario específico y cierra la conexión.
     * Limpia ambos mapas de sesiones (directo e inverso) para mantener consistencia.
     * Registra el evento de desconexión para monitoreo y debugging.
     *
     * @param userId Identificador único del usuario cuya sesión se debe remover
     */
    public void removeUserSession(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null) {
            sessionUsers.remove(session.getId());
            userSessions.remove(userId);
            try {
                session.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error cerrando sesión WebSocket para usuario " + userId + ": " + e.getMessage(), e);
            }
            logger.log(Level.INFO, "Usuario {0} desconectado de WebSocket", userId);
        }
    }
    
    /**
     * Remueve una sesión WebSocket utilizando su identificador de sesión.
     * Realiza lookup inverso para encontrar el userId asociado y luego remueve la sesión completa.
     * Si no encuentra asociación usuario-sesión, simplemente limpia el mapeo inverso.
     *
     * @param sessionId Identificador único de la sesión WebSocket a remover
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
     * Obtiene el número actual de usuarios con conexiones WebSocket activas.
     * Filtra solo las sesiones que están abiertas para proporcionar un conteo preciso.
     * Utilizado para monitoreo del sistema y métricas de uso en tiempo real.
     *
     * @return Número entero de usuarios conectados con sesiones WebSocket activas
     */
    public int getConnectedUsersCount() {
        return (int) userSessions.values().stream().filter(WebSocketSession::isOpen).count();
    }
    
    /**
     * Verifica si un usuario específico tiene una conexión WebSocket activa.
     * Comprueba tanto la existencia de la sesión como su estado de apertura.
     * Utilizado para determinar si se puede enviar una notificación en tiempo real.
     *
     * @param userId Identificador único del usuario a verificar
     * @return true si el usuario tiene una sesión WebSocket activa, false en caso contrario
     */
    public boolean isUserConnected(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * Crea un mensaje WebSocket estructurado en formato JSON.
     * Envuelve los datos en un objeto WebSocketMessage con tipo, contenido y timestamp.
     * Utiliza ObjectMapper para serialización automática a JSON.
     * En caso de error de serialización, retorna un mensaje de error mínimo.
     *
     * @param type Tipo de mensaje WebSocket (ej. "new_notification", "ping", "read_status_update")
     * @param data Objeto con los datos del mensaje a serializar
     * @return String en formato JSON con el mensaje estructurado
     */
    private String createWebSocketMessage(String type, Object data) {
        try {
            WebSocketMessage message = new WebSocketMessage(type, data, System.currentTimeMillis());
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error serializando mensaje WebSocket: " + e.getMessage(), e);
            return "{\"type\":\"error\",\"data\":{\"message\":\"Error serializando mensaje\"}}";
        }
    }
    
    /**
     * Clase DTO (Data Transfer Object) para estructurar mensajes WebSocket en formato JSON.
     * Utilizada para serialización automática de mensajes entre servidor y cliente.
     * Contiene tipo de mensaje, datos payload y timestamp para sincronización.
     *
     * Campos:
     * - type: Tipo de mensaje (ej. "new_notification", "ping", "connection_confirmed")
     * - data: Objeto con el contenido específico del mensaje
     * - timestamp: Unix timestamp en milisegundos para sincronización temporal
     */
    public static class WebSocketMessage {
        private String type;
        private Object data;
        private long timestamp;
        
        /**
         * Constructor por defecto requerido para deserialización JSON.
         */
        public WebSocketMessage() {}
        
        /**
         * Constructor completo para crear mensajes WebSocket estructurados.
         *
         * @param type Tipo de mensaje WebSocket
         * @param data Datos payload del mensaje
         * @param timestamp Timestamp de creación del mensaje
         */
        public WebSocketMessage(String type, Object data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        /**
         * Obtiene el tipo de mensaje WebSocket.
         *
         * @return Tipo de mensaje como String
         */
        public String getType() { return type; }
        
        /**
         * Establece el tipo de mensaje WebSocket.
         *
         * @param type Tipo de mensaje a establecer
         */
        public void setType(String type) { this.type = type; }
        
        /**
         * Obtiene los datos payload del mensaje.
         *
         * @return Objeto con los datos del mensaje
         */
        public Object getData() { return data; }
        
        /**
         * Establece los datos payload del mensaje.
         *
         * @param data Objeto con los datos a establecer
         */
        public void setData(Object data) { this.data = data; }
        
        /**
         * Obtiene el timestamp de creación del mensaje.
         *
         * @return Timestamp en milisegundos Unix
         */
        public long getTimestamp() { return timestamp; }
        
        /**
         * Establece el timestamp de creación del mensaje.
         *
         * @param timestamp Timestamp en milisegundos Unix a establecer
         */
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Clase DTO especializada para actualizaciones del estado de lectura de notificaciones.
     * Utilizada para comunicar cambios de estado de leída/no leída en tiempo real
     * a través de WebSocket cuando un usuario marca notificaciones como leídas.
     *
     * Campos:
     * - notificationId: ID único de la notificación cuyo estado cambió
     * - isRead: Nuevo estado de lectura (true = leída, false = no leída)
     */
    public static class ReadStatusUpdate {
        private Long notificationId;
        private boolean isRead;
        
        /**
         * Constructor por defecto requerido para deserialización JSON.
         */
        public ReadStatusUpdate() {}
        
        /**
         * Constructor para crear una actualización de estado de lectura.
         *
         * @param notificationId ID de la notificación cuyo estado cambió
         * @param isRead Nuevo estado de lectura
         */
        public ReadStatusUpdate(Long notificationId, boolean isRead) {
            this.notificationId = notificationId;
            this.isRead = isRead;
        }
        
        /**
         * Obtiene el ID de la notificación.
         *
         * @return ID de la notificación
         */
        public Long getNotificationId() { return notificationId; }
        
        /**
         * Establece el ID de la notificación.
         *
         * @param notificationId ID de la notificación a establecer
         */
        public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
        
        /**
         * Obtiene el estado de lectura.
         *
         * @return true si la notificación está leída, false si no
         */
        public boolean isRead() { return isRead; }
        
        /**
         * Establece el estado de lectura.
         *
         * @param read Estado de lectura a establecer
         */
        public void setRead(boolean read) { isRead = read; }
    }
}