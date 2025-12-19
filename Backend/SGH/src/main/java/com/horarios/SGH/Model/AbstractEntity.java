package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clase abstracta base para todas las entidades del sistema SGH.
 * Proporciona campos y métodos comunes como timestamps, validación
 * y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de proporcionar funcionalidades comunes a entidades
 * - OCP: Abierto para extensión por subclases
 *
 * Patrones de diseño aplicados:
 * - Template Method: Para operaciones comunes como validación y toString
 *
 * @author Sistema SGH
 * @version 1.0
 */
@MappedSuperclass
public abstract class AbstractEntity {

    /**
     * Timestamp de creación de la entidad.
     * Se establece automáticamente al crear la entidad.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    protected LocalDateTime createdAt;

    /**
     * Timestamp de última actualización de la entidad.
     * Se actualiza automáticamente en cada modificación.
     */
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    protected LocalDateTime updatedAt;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps.
     */
    public AbstractEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valida la entidad antes de persistirla.
     * Método Template Method a implementar por subclases.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    public abstract void validate();

    /**
     * Obtiene una representación resumida de la entidad.
     * Método Template Method a implementar por subclases.
     *
     * @return resumen como String
     */
    public abstract String getSummary();

    /**
     * Método de utilidad para logging y debugging.
     * Método Template Method a implementar por subclases.
     *
     * @return representación en string de la entidad
     */
    public abstract String toString();

    /**
     * Actualiza el timestamp de modificación.
     * Método de utilidad para marcar entidades como modificadas.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     * Método de utilidad para lógica de negocio.
     *
     * @return true si es nueva
     */
    public abstract boolean isNew();

    /**
     * Obtiene la fecha de creación de la entidad.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación de la entidad.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de última actualización de la entidad.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización de la entidad.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Verifica si la entidad ha sido modificada recientemente.
     *
     * @param minutes minutos para considerar como reciente
     * @return true si fue modificada recientemente
     */
    public boolean isRecentlyModified(int minutes) {
        return updatedAt.isAfter(LocalDateTime.now().minusMinutes(minutes));
    }

    /**
     * Obtiene la antigüedad de la entidad en días.
     *
     * @return número de días desde la creación
     */
    public long getAgeInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }
}