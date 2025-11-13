package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para el manejo de notificaciones In-App
 * Proporciona acceso a la base de datos para operaciones CRUD sobre InAppNotification
 */
@Repository
public interface IInAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    
    /**
     * Busca notificaciones por usuario activo (no archivadas y no expiradas)
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.isArchived = false " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) ORDER BY n.createdAt DESC")
    Page<InAppNotification> findActiveByUserId(@Param("userId") Integer userId, 
                                              @Param("now") LocalDateTime now, 
                                              Pageable pageable);
    
    /**
     * Busca notificaciones no leídas por usuario
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.isRead = false " +
           "AND n.isArchived = false AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<InAppNotification> findUnreadByUserId(@Param("userId") Integer userId, 
                                              @Param("now") LocalDateTime now);
    
    /**
     * Cuenta notificaciones no leídas por usuario
     */
    @Query("SELECT COUNT(n) FROM in_app_notifications n WHERE n.userId = :userId AND n.isRead = false " +
           "AND n.isArchived = false AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    Long countUnreadByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
    
    /**
     * Busca notificaciones por tipo y usuario
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.notificationType = :type " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndType(@Param("userId") Integer userId, 
                                               @Param("type") NotificationType type, 
                                               Pageable pageable);
    
    /**
     * Busca notificaciones por prioridad
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.priority = :priority " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndPriority(@Param("userId") Integer userId, 
                                                   @Param("priority") NotificationPriority priority, 
                                                   Pageable pageable);
    
    /**
     * Busca notificaciones por categoría
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.category = :category " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndCategory(@Param("userId") Integer userId, 
                                                   @Param("category") String category, 
                                                   Pageable pageable);
    
    /**
     * Busca notificaciones recientes (últimas 24 horas)
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId " +
           "AND n.createdAt >= :since AND n.isArchived = false " +
           "ORDER BY n.createdAt DESC")
    List<InAppNotification> findRecentByUserId(@Param("userId") Integer userId, 
                                             @Param("since") LocalDateTime since);
    
    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    @Query("UPDATE in_app_notifications n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
    
    /**
     * Marca una notificación específica como leída
     */
    @Query("UPDATE in_app_notifications n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.notificationId = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("now") LocalDateTime now);
    
    /**
     * Archiva notificaciones antiguas (más de días especificados)
     */
    @Query("UPDATE in_app_notifications n SET n.isArchived = true " +
           "WHERE n.userId = :userId AND n.createdAt < :cutoffDate AND n.isArchived = false")
    void archiveOldByUserId(@Param("userId") Integer userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Elimina notificaciones expiradas
     */
    @Query("DELETE FROM in_app_notifications n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
    
    /**
     * Busca notificaciones que requieren atención inmediata (alta prioridad y no leídas)
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.priority IN :highPriorities " +
           "AND n.isRead = false AND n.isArchived = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<InAppNotification> findHighPriorityUnreadByUserId(@Param("userId") Integer userId, 
                                                          @Param("highPriorities") List<NotificationPriority> highPriorities);
    
    /**
     * Obtiene estadísticas de notificaciones por usuario
     */
    @Query("SELECT n.priority, COUNT(n) FROM in_app_notifications n WHERE n.userId = :userId " +
           "AND n.isArchived = false GROUP BY n.priority")
    List<Object[]> getPriorityStatsByUserId(@Param("userId") Integer userId);
    
    /**
     * Busca notificaciones por múltiples criterios
     */
    @Query("SELECT n FROM in_app_notifications n WHERE " +
           "(:userId IS NULL OR n.userId = :userId) AND " +
           "(:type IS NULL OR n.notificationType = :type) AND " +
           "(:priority IS NULL OR n.priority = :priority) AND " +
           "(:category IS NULL OR n.category = :category) AND " +
           "(:isRead IS NULL OR n.isRead = :isRead) AND " +
           "(:isArchived IS NULL OR n.isArchived = :isArchived) AND " +
           "(n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    Page<InAppNotification> findWithFilters(@Param("userId") Integer userId,
                                           @Param("type") NotificationType type,
                                           @Param("priority") NotificationPriority priority,
                                           @Param("category") String category,
                                           @Param("isRead") Boolean isRead,
                                           @Param("isArchived") Boolean isArchived,
                                           @Param("now") LocalDateTime now,
                                           Pageable pageable);
}