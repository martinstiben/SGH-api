package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.InAppNotification;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para notificaciones In-App siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de notificaciones.
 *
 * Implementa el patrón Repository con consultas optimizadas y el patrón Factory a través de RepositoryFactory
 * para consultas dinámicas. Aplica el patrón Specification para filtros complejos.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de notificaciones In-App
 * - OCP: Extensible mediante Specifications y Factory
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para notificaciones
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IInAppNotificationRepository extends AbstractRepository<InAppNotification, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para notificaciones, considera "activas" aquellas no archivadas.
     */
    @Override
    @Query("SELECT n FROM in_app_notifications n WHERE n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca notificaciones por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT n FROM in_app_notifications n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Page<InAppNotification> findByCreatedDateBetween(LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta notificaciones activas (no archivadas).
     */
    @Override
    @Query("SELECT COUNT(n) FROM in_app_notifications n WHERE n.isArchived = false")
    long countActive();

    /**
     * {@inheritDoc}
     * Verifica si existe una notificación activa con el ID especificado.
     */
    @Override
    @Query("SELECT COUNT(n) > 0 FROM in_app_notifications n WHERE n.notificationId = :id AND n.isArchived = false")
    boolean existsActiveById(Long id);

    /**
     * {@inheritDoc}
     * Busca una notificación activa por su ID.
     */
    @Override
    @Query("SELECT n FROM in_app_notifications n WHERE n.notificationId = :id AND n.isArchived = false")
    Optional<InAppNotification> findActiveById(Long id);

    /**
     * {@inheritDoc}
     * Busca notificaciones por términos de búsqueda en título o mensaje.
     */
    @Override
    @Query("SELECT n FROM in_app_notifications n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<InAppNotification> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca notificaciones activas por usuario (no archivadas).
     * Método optimizado para dashboard de usuario.
     *
     * @param userId ID del usuario
     * @param pageable configuración de paginación
     * @return página de notificaciones activas del usuario
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.isArchived = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    Page<InAppNotification> findActiveByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Busca notificaciones no leídas por usuario
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.isRead = false " +
           "AND n.isArchived = false AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<InAppNotification> findUnreadByUserId(@Param("userId") Long userId,
                                              @Param("now") LocalDateTime now);
    
    /**
     * Cuenta notificaciones no leídas por usuario
     */
    @Query("SELECT COUNT(n) FROM in_app_notifications n WHERE n.userId = :userId AND n.isRead = false " +
           "AND n.isArchived = false AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    Long countUnreadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Busca notificaciones por tipo y usuario
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.notificationType = :type " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndType(@Param("userId") Long userId,
                                               @Param("type") NotificationType type,
                                               Pageable pageable);
    
    /**
     * Busca notificaciones por prioridad
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.priority = :priority " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndPriority(@Param("userId") Long userId,
                                                    @Param("priority") NotificationPriority priority,
                                                    Pageable pageable);
    
    /**
     * Busca notificaciones por categoría
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.category = :category " +
           "AND n.isArchived = false ORDER BY n.createdAt DESC")
    Page<InAppNotification> findByUserIdAndCategory(@Param("userId") Long userId,
                                                    @Param("category") String category,
                                                    Pageable pageable);
    
    /**
     * Busca notificaciones recientes (últimas 24 horas)
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId " +
           "AND n.createdAt >= :since AND n.isArchived = false " +
           "ORDER BY n.createdAt DESC")
    List<InAppNotification> findRecentByUserId(@Param("userId") Long userId,
                                               @Param("since") LocalDateTime since);
    
    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    @Modifying
    @Transactional
    @Query("UPDATE in_app_notifications n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Marca una notificación específica como leída
     */
    @Modifying
    @Transactional
    @Query("UPDATE in_app_notifications n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.notificationId = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("now") LocalDateTime now);
    
    /**
     * Archiva notificaciones antiguas (más de días especificados)
     */
    @Modifying
    @Transactional
    @Query("UPDATE in_app_notifications n SET n.isArchived = true " +
           "WHERE n.userId = :userId AND n.createdAt < :cutoffDate AND n.isArchived = false")
    void archiveOldByUserId(@Param("userId") Long userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Elimina notificaciones expiradas
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM in_app_notifications n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
    
    /**
     * Busca notificaciones que requieren atención inmediata (alta prioridad y no leídas)
     */
    @Query("SELECT n FROM in_app_notifications n WHERE n.userId = :userId AND n.priority IN :highPriorities " +
           "AND n.isRead = false AND n.isArchived = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<InAppNotification> findHighPriorityUnreadByUserId(@Param("userId") Long userId,
                                                           @Param("highPriorities") List<NotificationPriority> highPriorities);
    
    /**
     * Obtiene estadísticas de notificaciones por usuario
     */
    @Query("SELECT n.priority, COUNT(n) FROM in_app_notifications n WHERE n.userId = :userId " +
           "AND n.isArchived = false GROUP BY n.priority")
    List<Object[]> getPriorityStatsByUserId(@Param("userId") Long userId);
    
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
    Page<InAppNotification> findWithFilters(@Param("userId") Long userId,
                                            @Param("type") NotificationType type,
                                            @Param("priority") NotificationPriority priority,
                                            @Param("category") String category,
                                            @Param("isRead") Boolean isRead,
                                            @Param("isArchived") Boolean isArchived,
                                            @Param("now") LocalDateTime now,
                                            Pageable pageable);
}