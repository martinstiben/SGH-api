package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.DTO.InAppNotificationResponseDTO;
import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.NotificationPriority;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Repository.IInAppNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Servicio para el manejo de notificaciones In-App en tiempo real
 * Integra con WebSocket para sincronización en React web y React Native móvil
 */
@Slf4j
@Service
public class InAppNotificationService {
    
    @Autowired
    private IInAppNotificationRepository inAppNotificationRepository;
    
    @Autowired
    private usersService userService;
    
    /**
     * Envía notificación In-App y la distribuye en tiempo real vía WebSocket
     */
    @Async
    public CompletableFuture<InAppNotification> sendInAppNotificationAsync(InAppNotificationDTO notificationDTO) {
        log.info("Enviando notificación In-App a usuario {}: {}", notificationDTO.getUserId(), notificationDTO.getTitle());
        
        try {
            // Buscar información del usuario
            users user = userService.findById(notificationDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + notificationDTO.getUserId()));
            
            // Convertir tipos de datos
            NotificationType type = NotificationType.valueOf(notificationDTO.getNotificationType());
            NotificationPriority priority = NotificationPriority.valueOf(notificationDTO.getPriority());

            // Crear notificación
            InAppNotification notification = new InAppNotification(
                user.getUserId(),
                user.getPerson().getEmail(),
                user.getPerson().getFullName(),
                user.getRole().getRoleName(),
                type,
                notificationDTO.getTitle(),
                notificationDTO.getMessage()
            );

            // Configurar campos adicionales
            notification.setPriority(priority);
            notification.setCategory(notificationDTO.getCategory());
            notification.setActionUrl(notificationDTO.getActionUrl());
            notification.setActionText(notificationDTO.getActionText());
            notification.setIcon(notificationDTO.getIcon());
            
            // Guardar en base de datos
            InAppNotification savedNotification = inAppNotificationRepository.save(notification);
            
            // Enviar vía WebSocket en tiempo real (comentado hasta que WebSocket esté compilado)
            // InAppNotificationDTO dto = convertToDTO(savedNotification);
            // webSocketService.sendNotificationToUser(String.valueOf(user.getUserId()), dto);
            
            log.info("Notificación In-App guardada exitosamente para usuario {}: {}",
                    notificationDTO.getUserId(), notificationDTO.getTitle());
            
            return CompletableFuture.completedFuture(savedNotification);
            
        } catch (Exception e) {
            log.error("Error al enviar notificación In-App a usuario {}: {}",
                     notificationDTO.getUserId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Obtiene notificaciones activas de un usuario
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getActiveNotificationsByUserId(Integer userId, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size);
        return inAppNotificationRepository.findActiveByUserId(userId, now, pageable);
    }
    
    /**
     * Obtiene notificaciones no leídas de un usuario
     */
    @Transactional(readOnly = true)
    public List<InAppNotification> getUnreadNotificationsByUserId(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        return inAppNotificationRepository.findUnreadByUserId(userId, now);
    }
    
    /**
     * Cuenta notificaciones no leídas de un usuario
     */
    @Transactional(readOnly = true)
    public Long countUnreadNotificationsByUserId(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        return inAppNotificationRepository.countUnreadByUserId(userId, now);
    }
    
    /**
     * Marca una notificación como leída
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        LocalDateTime now = LocalDateTime.now();
        inAppNotificationRepository.markAsRead(notificationId, now);
    }
    
    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        inAppNotificationRepository.markAllAsReadByUserId(userId, now);
        
        // Enviar confirmación vía WebSocket (comentado hasta que WebSocket esté compilado)
        // webSocketService.sendBulkReadStatusToUser(String.valueOf(userId));
    }
    
    /**
     * Busca notificaciones por tipo y usuario
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getNotificationsByTypeAndUser(Integer userId, NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inAppNotificationRepository.findByUserIdAndType(userId, type, pageable);
    }
    
    /**
     * Busca notificaciones por prioridad y usuario
     */
    @Transactional(readOnly = true)
    public Page<InAppNotification> getNotificationsByPriorityAndUser(Integer userId, NotificationPriority priority, int page, int size) {
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
        dto.setUserEmail(notification.getUserEmail());
        dto.setUserName(notification.getUserName());
        dto.setUserRole(notification.getUserRole());
        dto.setNotificationType(notification.getNotificationType().name());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setActionUrl(notification.getActionUrl());
        dto.setActionText(notification.getActionText());
        dto.setIcon(notification.getIcon());
        dto.setPriority(notification.getPriority().name());
        dto.setCategory(notification.getCategory());
        dto.setRead(notification.isRead());
        dto.setArchived(notification.isArchived());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        dto.setExpiresAt(notification.getExpiresAt());

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
            log.warn("Error al convertir metadata a JSON: {}", e.getMessage());
            return null;
        }
    }
}