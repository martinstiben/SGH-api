package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa una materia en el sistema SGH.
 * Una materia puede ser impartida por múltiples docentes y forma parte
 * del sistema de gestión académica para la organización curricular.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar materias
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractEntity
 *
 * Patrones de diseño aplicados:
 * - Template Method: Implementado a través de AbstractEntity
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name="subjects")
public class subjects extends AbstractEntity {

    /**
     * Identificador único de la materia.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="subjectId")
    private int id;

    /**
     * Nombre de la materia (ej: "Matemáticas", "Física").
     * Debe ser único y contener solo letras y espacios.
     */
    @Column(name="subjectName", nullable=false, unique=true, length = 100)
    @NotNull(message = "El nombre de la materia no puede ser nulo")
    @NotBlank(message = "El nombre de la materia no puede estar vacío")
    @Size(min = 4, max = 100, message = "El nombre de la materia debe tener entre 4 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre de la materia solo puede contener letras y espacios")
    private String subjectName;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public subjects() {
        super();
    }

    /**
     * Constructor con parámetros básicos para creación de materias.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param id identificador único de la materia
     * @param subjectName nombre de la materia
     */
    public subjects(int id, String subjectName) {
        super();
        this.id = id;
        this.subjectName = subjectName;
    }

    /**
     * Obtiene el identificador único de la materia.
     *
     * @return ID de la materia
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único de la materia.
     *
     * @param id ID de la materia
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre de la materia.
     *
     * @return nombre de la materia
     */
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * Establece el nombre de la materia.
     *
     * @param subjectName nombre de la materia
     */
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que el nombre de la materia sea válido según las restricciones
     * de negocio definidas.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (subjectName == null || subjectName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la materia no puede estar vacío");
        }
        if (subjectName.length() < 4 || subjectName.length() > 100) {
            throw new IllegalArgumentException("El nombre de la materia debe tener entre 4 y 100 caracteres");
        }
        if (!subjectName.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
            throw new IllegalArgumentException("El nombre de la materia solo puede contener letras y espacios");
        }
    }

    /**
     * Obtiene una representación resumida de la materia.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Materia: " + subjectName + " (ID: " + id + ")";
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return id == 0;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string de la materia
     */
    @Override
    public String toString() {
        return "subjects{" +
                "id=" + id +
                ", subjectName='" + subjectName + '\'' +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}