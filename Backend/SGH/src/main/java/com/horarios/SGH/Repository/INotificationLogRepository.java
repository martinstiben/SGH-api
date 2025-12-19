package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.NotificationLog;
import com.horarios.SGH.Model.NotificationStatus;
import com.horarios.SGH.Model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para logs de notificaciones siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de logging.
 *
 * Implementa el patrón Repository con consultas optimizadas para auditoría y monitoreo de notificaciones.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas de logs.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de logs de notificaciones
 * - OCP: Extensible mediante Specifications para filtros de auditoría
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para logs de notificación
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes de auditoría
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de logs
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface INotificationLogRepository extends AbstractRepository<NotificationLog, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para logs de notificación, considera "activos" todos los registros (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT nl FROM notification_logs nl ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca logs por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT nl FROM notification_logs nl WHERE nl.createdAt BETWEEN :startDate AND :endDate")
    Page<NotificationLog> findByCreatedDateBetween(LocalDateTime startDate,
                                                  LocalDateTime endDate,
                                                  Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los logs (no hay concepto de "inactivos" en logs).
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los logs siempre existen si tienen ID (no hay estado activo/inactivo).
     */
    @Override
    default boolean existsActiveById(Long id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los logs siempre están "activos" (no hay eliminación lógica).
     */
    @Override
    default Optional<NotificationLog> findActiveById(Long id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca logs por términos de búsqueda en asunto o contenido.
     */
    @Override
    @Query("SELECT nl FROM notification_logs nl WHERE " +
           "LOWER(nl.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nl.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nl.recipientEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<NotificationLog> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca logs de notificaciones pendientes de envío.
     * Método optimizado para cola de procesamiento.
     *
     * @param status estado de las notificaciones a buscar
     * @param pageable configuración de paginación
     * @return página de logs pendientes
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