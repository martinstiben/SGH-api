package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.NotificationLog;
import com.horarios.SGH.Model.NotificationStatus;
import com.horarios.SGH.Model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para el manejo de logs de notificaciones
 * Proporciona acceso a la base de datos para operaciones CRUD sobre NotificationLog
 */
@Repository
public interface INotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    /**
     * Busca notificaciones pendientes de envío
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.status = :status ORDER BY nl.createdAt ASC")
    Page<NotificationLog> findPendingNotifications(@Param("status") NotificationStatus status, Pageable pageable);
    
    /**
     * Busca notificaciones por destinatario
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.recipientEmail = :email ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByRecipientEmail(@Param("email") String email, Pageable pageable);

    /**
     * Busca notificaciones recientes por destinatario (últimas 24 horas)
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.recipientEmail = :email AND nl.createdAt >= :since ORDER BY nl.createdAt DESC")
    List<NotificationLog> findRecentByRecipientEmail(@Param("email") String email, @Param("since") LocalDateTime since);
    
    /**
     * Busca notificaciones por tipo
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.notificationType = :type ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByNotificationType(@Param("type") NotificationType type, Pageable pageable);
    
    /**
     * Busca notificaciones por rol del destinatario
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.recipientRole = :role ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByRecipientRole(@Param("role") String role, Pageable pageable);
    
    /**
     * Busca notificaciones fallidas que pueden reintentarse
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.status = :status AND nl.attemptsCount < nl.maxAttempts ORDER BY nl.createdAt ASC")
    List<NotificationLog> findFailedNotificationsToRetry(@Param("status") NotificationStatus status);
    
    /**
     * Busca notificaciones por estado en un rango de fechas
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.status = :status AND nl.createdAt BETWEEN :startDate AND :endDate ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByStatusAndDateRange(@Param("status") NotificationStatus status, 
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate, 
                                                   Pageable pageable);
    
    /**
     * Cuenta las notificaciones por tipo y estado
     */
    @Query("SELECT COUNT(nl) FROM notification_logs nl WHERE nl.notificationType = :type AND nl.status = :status")
    Long countByTypeAndStatus(@Param("type") NotificationType type, @Param("status") NotificationStatus status);
    
    /**
     * Obtiene estadísticas de notificaciones del día
     */
    @Query("SELECT nl.status, COUNT(nl) FROM notification_logs nl WHERE nl.createdAt >= :startOfDay GROUP BY nl.status")
    List<Object[]> getNotificationStatsForDay(@Param("startOfDay") LocalDateTime startOfDay);
    
    /**
     * Busca notificaciones recientes de un destinatario
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.recipientEmail = :email AND nl.createdAt >= :since ORDER BY nl.createdAt DESC")
    List<NotificationLog> findRecentNotifications(@Param("email") String email, @Param("since") LocalDateTime since);
    
    /**
     * Elimina notificaciones antiguas (más de días especificados)
     */
    @Query("DELETE FROM notification_logs nl WHERE nl.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Busca notificaciones que necesitan reintento después de un tiempo específico
     */
    @Query("SELECT nl FROM notification_logs nl WHERE nl.status = :status AND nl.attemptsCount < nl.maxAttempts AND nl.lastAttempt <= :retryAfter ORDER BY nl.lastAttempt ASC")
    List<NotificationLog> findNotificationsReadyForRetry(@Param("status") NotificationStatus status, @Param("retryAfter") LocalDateTime retryAfter);
}