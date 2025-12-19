package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa un docente en el sistema SGH.
 * Un docente puede impartir múltiples materias y tener disponibilidad horaria,
 * formando parte del sistema de gestión académica y recursos humanos.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar docentes
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
@Entity(name="teachers")
public class teachers extends AbstractEntity {

    /**
     * Identificador único del docente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="teacherId")
    private int id;

    /**
     * Nombre completo del docente.
     * Solo puede contener letras y espacios.
     */
    @Column(name="teacherName", length = 100, nullable=false)
    @NotBlank(message = "El nombre del profesor no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre del profesor debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre del profesor solo puede contener letras y espacios")
    private String teacherName;

    /**
     * Datos binarios de la foto de perfil del docente.
     */
    @Column(name="photoData", columnDefinition = "MEDIUMBLOB")
    @Lob
    private byte[] photoData;

    /**
     * Tipo de contenido de la foto (ej: image/jpeg).
     */
    @Column(name="photoContentType", length = 100)
    private String photoContentType;

    /**
     * Nombre del archivo de la foto.
     */
    @Column(name="photoFileName", length = 255)
    private String photoFileName;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public teachers() {
        super();
    }

    /**
     * Constructor con parámetros básicos para creación de docentes.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param id identificador único del docente
     * @param teacherName nombre completo del docente
     */
    public teachers(int id, String teacherName) {
        super();
        this.id = id;
        this.teacherName = teacherName;
    }

    /**
     * Obtiene el identificador único del docente.
     *
     * @return ID del docente
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del docente.
     *
     * @param id ID del docente
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre completo del docente.
     *
     * @return nombre del docente
     */
    public String getTeacherName() {
        return teacherName;
    }

    /**
     * Establece el nombre completo del docente.
     *
     * @param teacherName nombre del docente
     */
    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    /**
     * Obtiene los datos binarios de la foto de perfil del docente.
     *
     * @return datos binarios de la foto
     */
    public byte[] getPhotoData() {
        return photoData;
    }

    /**
     * Establece los datos binarios de la foto de perfil del docente.
     *
     * @param photoData datos binarios de la foto
     */
    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    /**
     * Obtiene el tipo de contenido de la foto de perfil.
     *
     * @return tipo MIME de la foto (ej: "image/jpeg")
     */
    public String getPhotoContentType() {
        return photoContentType;
    }

    /**
     * Establece el tipo de contenido de la foto de perfil.
     *
     * @param photoContentType tipo MIME de la foto
     */
    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    /**
     * Obtiene el nombre del archivo de la foto de perfil.
     *
     * @return nombre del archivo de la foto
     */
    public String getPhotoFileName() {
        return photoFileName;
    }

    /**
     * Establece el nombre del archivo de la foto de perfil.
     *
     * @param photoFileName nombre del archivo de la foto
     */
    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que el nombre del docente sea válido según las restricciones
     * de negocio definidas.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del profesor no puede estar vacío");
        }
        if (teacherName.length() < 2 || teacherName.length() > 100) {
            throw new IllegalArgumentException("El nombre del profesor debe tener entre 2 y 100 caracteres");
        }
        if (!teacherName.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
            throw new IllegalArgumentException("El nombre del profesor solo puede contener letras y espacios");
        }
    }

    /**
     * Obtiene una representación resumida del docente.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Docente: " + teacherName + " (ID: " + id + ")";
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
     * @return representación en string del docente
     */
    @Override
    public String toString() {
        return "teachers{" +
                "id=" + id +
                ", teacherName='" + teacherName + '\'' +
                ", hasPhoto=" + (photoData != null && photoData.length > 0) +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }

    /**
     * Verifica si el docente tiene una foto de perfil configurada.
     *
     * @return true si tiene foto
     */
    public boolean hasPhoto() {
        return photoData != null && photoData.length > 0;
    }

    /**
     * Obtiene el tamaño de la foto en bytes.
     *
     * @return tamaño de la foto en bytes, o 0 si no tiene foto
     */
    public long getPhotoSize() {
        return hasPhoto() ? photoData.length : 0;
    }
}