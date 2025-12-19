package com.horarios.SGH.DTO;

import java.time.LocalDateTime;

/**
 * Clase abstracta base para todos los DTOs del sistema SGH.
 * Implementa el patrón Abstract Factory proporcionando una interfaz común
 * para creación y validación de DTOs.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de proporcionar funcionalidades comunes a DTOs
 * - OCP: Abierto para extensión por subclases
 * - LSP: Las subclases pueden ser usadas donde se espera AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Para creación centralizada de DTOs
 * - Template Method: Para validación y resúmenes
 *
 * @author Sistema SGH
 * @version 1.0
 */
public abstract class AbstractDTO {

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método Template Method a implementar por subclases.
     *
     * @return true si es válido
     */
    public abstract boolean isValid();

    /**
     * Obtiene una representación resumida del DTO.
     * Método Template Method a implementar por subclases.
     *
     * @return resumen como String
     */
    public abstract String getSummary();

    /**
     * Método Factory para crear un DTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     * Debe ser sobrescrito por subclases concretas.
     *
     * @return DTO vacío
     */
    public static AbstractDTO empty() {
        throw new UnsupportedOperationException("Debe ser implementado por subclases");
    }

    /**
     * Método Factory genérico para crear DTOs vacíos.
     * Implementa patrón Abstract Factory para creación centralizada.
     *
     * @param dtoClass Clase del DTO a crear
     * @return DTO vacío de la clase especificada
     */
    public static <T extends AbstractDTO> T createEmpty(Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            dto.setCreatedAt(LocalDateTime.now());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Error creando DTO vacío para clase: " + dtoClass.getSimpleName(), e);
        }
    }

    // Getters y setters para timestamps
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}