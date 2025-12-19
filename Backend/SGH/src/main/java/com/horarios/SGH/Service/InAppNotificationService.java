package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.DTO.InAppNotificationResponseDTO;
import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.NotificationCategory;
import com.horarios.SGH.Model.NotificationPriority;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.User;
import com.horarios.SGH.Repository.IInAppNotificationRepository;
import java.util.logging.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para el manejo de notificaciones In-App en tiempo real.
 * Gestiona notificaciones dentro de la aplicación con integración WebSocket.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de gestionar notificaciones In-App
 * - OCP: Abierto para extensión de tipos de notificación
 * - DIP: Depende de abstracciones (repositorios, servicios)
 *
 * Funcionalidades:
 * - Envío asíncrono de notificaciones
 * - Consulta paginada de notificaciones
 * - Marcado como leído
 * - Integración con WebSocket para tiempo real
 * - Categorización y priorización de notificaciones
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class InAppNotificationService {
    
    private final IInAppNotificationRepository inAppNotificationRepository;
    private final usersService userService;
    
    /**
     * Logger para registro de eventos del servicio de notificaciones in-app.
     */
    private static final Logger logger = Logger.getLogger(InAppNotificationService.class.getName());
    
    /**
     * Logger estático para compatibilidad con código existente.
     */
    private static final Logger log = logger;
    
    /**
     * Constructor manual para inyección de dependencias.
     * Mantiene compatibilidad con Spring y permite testing.
     *
     * @param inAppNotificationRepository Repositorio de notificaciones in-app
     * @param userService Servicio de usuarios
     */
    public InAppNotificationService(IInAppNotificationRepository inAppNotificationRepository,
                                   usersService userService) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.userService = userService;
    }
    
    /**
     * Envía notificación In-App de forma asíncrona y la distribuye en tiempo real vía WebSocket.
     * Valida el usuario destinatario y crea la notificación en base de datos.
     *
     * @param notificationDTO DTO con los datos de la notificación a enviar
     * @return CompletableFuture con la notificación creada
     * @throws IllegalArgumentException si el usuario no existe
     */
    @Async
    public CompletableFuture<InAppNotification> sendInAppNotificationAsync(InAppNotificationDTO notificationDTO) {
        logger.info("Enviando notificación In-App a usuario " + notificationDTO.getUserId() + ": " + notificationDTO.getTitle());
        
        try {
            // Buscar información del usuario
            User user = userService.findById(notificationDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + notificationDTO.getUserId()));
            
            // Convertir tipos de datos
            NotificationType type = NotificationType.valueOf(notificationDTO.getNotificationType());
            NotificationPriority priority = NotificationPriority.valueOf(notificationDTO.getPriority());

            // Crear notificación
            InAppNotification notification = new InAppNotification(
                user.getUserId(),
                type,
                notificationDTO.getTitle(),
                notificationDTO.getMessage()
            );

            // Configurar campos adicionales
            notification.setPriority(priority);
            notification.setCategory(NotificationCategory.valueOf(notificationDTO.getCategory()));
            notification.setActionUrl(notificationDTO.getActionUrl());
            notification.setActionText(notificationDTO.getActionText());
            notification.setIcon(notificationDTO.getIcon());
            
            // Guardar en base de datos
            InAppNotification savedNotification = inAppNotificationRepository.save(notification);
            
            // Enviar vía WebSocket en tiempo real (comentado hasta que WebSocket esté compilado)
            // InAppNotificationDTO dto = convertToDTO(savedNotification);
            // webSocketService.sendNotificationToUser(String.valueOf(user.getUserId()), dto);
            
            logger.info("Notificación In-App guardada exitosamente para usuario " +
                    notificationDTO.getUserId() + ": " + notificationDTO.getTitle());
            
            return CompletableFuture.completedFuture(savedNotification);
            
        } catch (Exception e) {
            logger.severe("Error al enviar notificación In-App a usuario " +
                     notificationDTO.getUserId() + ": " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Obtiene notificaciones activas de un usuario con paginación.
     * Las notificaciones activas son aquellas no expiradas y no archivadas.
     *
     * @param userId ID del usuario
     * @param page Número de página (0-based)
     * @param size Tamaño de página
     * @return Página de notificaciones activas
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getActiveNotificationsByUserId(Long userId, int page, int size) {
        logger.info("Buscando notificaciones activas para usuario " + userId + " (página: " + page + ", tamaño: " + size + ")");
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size);
        Page<InAppNotification> result = inAppNotificationRepository.findActiveByUserId(userId, pageable);
        logger.info("Encontradas " + result.getTotalElements() + " notificaciones activas para usuario " + userId);
        return result;
    }

    /**
     * Obtiene todas las notificaciones no leídas de un usuario.
     *
     * @param userId ID del usuario
     * @return Lista de notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public List<InAppNotification> getUnreadNotificationsByUserId(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return inAppNotificationRepository.findUnreadByUserId(userId, now);
    }

    /**
     * Cuenta el número de notificaciones no leídas de un usuario.
     *
     * @param userId ID del usuario
     * @return Número de notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public Long countUnreadNotificationsByUserId(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return inAppNotificationRepository.countUnreadByUserId(userId, now);
    }

    /**
     * Marca una notificación específica como leída.
     * Actualiza la fecha de lectura y cambia el estado.
     *
     * @param notificationId ID de la notificación a marcar como leída
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Marcando notificación " + notificationId + " como leída");
        inAppNotificationRepository.markAsRead(notificationId, now);
        logger.info("Notificación " + notificationId + " marcada como leída exitosamente");
    }

    /**
     * Marca todas las notificaciones activas de un usuario como leídas.
     * Actualiza masivamente todas las notificaciones no leídas del usuario.
     *
     * @param userId ID del usuario
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        inAppNotificationRepository.markAllAsReadByUserId(userId, now);

        // Enviar confirmación vía WebSocket (comentado hasta que WebSocket esté compilado)
        // webSocketService.sendBulkReadStatusToUser(String.valueOf(userId));
    }

    /**
     * Obtiene una notificación específica por su ID.
     *
     * @param notificationId ID de la notificación
     * @return La notificación encontrada
     * @throws IllegalArgumentException si la notificación no existe
     */
    @Transactional(readOnly = true)
    public InAppNotification getNotificationById(Long notificationId) {
        return inAppNotificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada: " + notificationId));
    }

    /**
     * Obtiene el conteo de notificaciones no leídas de un usuario.
     * Método de conveniencia que delega a countUnreadNotificationsByUserId.
     *
     * @param userId ID del usuario
     * @return Número de notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return inAppNotificationRepository.countUnreadByUserId(userId, now);
    }
    
    /**
     * Busca notificaciones por tipo y usuario
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getNotificationsByTypeAndUser(Long userId, NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inAppNotificationRepository.findByUserIdAndType(userId, type, pageable);
    }
    
    /**
     * Busca notificaciones por prioridad y usuario
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getNotificationsByPriorityAndUser(Long userId, NotificationPriority priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inAppNotificationRepository.findByUserIdAndPriority(userId, priority, pageable);
    }
    
    /**
     * Convierte InAppNotification a InAppNotificationResponseDTO
     */
    private InAppNotificationResponseDTO convertToDTO(InAppNotification notification) {
        InAppNotificationResponseDTO dto = new InAppNotificationResponseDTO();

        dto.setNotificationId(notification.getNotificationId());
        dto.setUserId(notification.getUserId());
        dto.setNotificationType(notification.getNotificationType().name());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setActionUrl(notification.getActionUrl());
        dto.setActionText(notification.getActionText());
        dto.setIcon(notification.getIcon());
        dto.setPriority(notification.getPriority().name());
        dto.setCategory(notification.getCategory().name());
        dto.setRead(notification.isRead());
        dto.setArchived(notification.isArchived());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());

        // Campos calculados
        dto.setPriorityDisplayName(notification.getPriority().getDisplayName());
        dto.setPriorityColor(notification.getPriority().getColor());
        dto.setPriorityIcon(notification.getPriority().getIcon());
        dto.setAge(notification.getAge());
        dto.setRecent(notification.isRecent());
        dto.setActive(notification.isActive());
        dto.setRequiresImmediateAttention(notification.getPriority().requiresImmediateAttention());

        return dto;
    }
    
    /**
     * Convierte Map de metadata a String JSON
     */
    private String convertMetadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(metadata);
        } catch (Exception e) {
            logger.warning("Error al convertir metadata a JSON: " + e.getMessage());
            return null;
        }
    }
}
