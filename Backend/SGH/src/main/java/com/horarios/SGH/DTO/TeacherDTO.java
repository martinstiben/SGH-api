package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de docentes del sistema SGH.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones específicas para docentes.
 *
 * Proporciona una representación completa de la entidad Teacher
 * incluyendo información de disponibilidad, asignaturas y fotos de perfil.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar docentes
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Implementado a través de AbstractDTO
 * - Factory Method: Para creación de instancias
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherDTO extends AbstractDTO {

    /**
     * Identificador único del docente en el sistema.
     * Generado automáticamente por la base de datos.
     */
    private int teacherId;

    /**
     * Nombre completo del docente.
     * Debe contener solo letras y espacios, sin números.
     */
    @NotBlank(message = "El nombre del profesor no puede estar vacío")
    @Size(min = 5, max = 50, message = "El nombre del profesor debe tener entre 5 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre del profesor solo puede contener letras y espacios")
    private String teacherName;

    /**
     * Identificador de la asignatura principal del docente.
     * Para compatibilidad con el servicio existente.
     */
    private int subjectId;

    /**
     * Resumen textual de la disponibilidad horaria del docente.
     * Formato: "Lunes, Miércoles, Viernes" o "Sin disponibilidad"
     */
    private String availabilitySummary;

    /**
     * Datos binarios de la foto de perfil del docente.
     * Almacenados como array de bytes.
     */
    private byte[] photoData;

    /**
     * Tipo MIME de la foto de perfil (ej: "image/jpeg", "image/png").
     */
    private String photoContentType;

    /**
     * Nombre del archivo original de la foto de perfil.
     */
    private String photoFileName;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public TeacherDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param teacherId ID del docente
     * @param teacherName Nombre del docente
     */
    public TeacherDTO(int teacherId, String teacherName) {
        super();
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getAvailabilitySummary() {
        return availabilitySummary;
    }

    public void setAvailabilitySummary(String availabilitySummary) {
        this.availabilitySummary = availabilitySummary;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Método Factory para crear un TeacherDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param teacherId ID del docente
     * @param teacherName Nombre del docente
     * @return TeacherDTO con los datos básicos
     */
    public static TeacherDTO create(int teacherId, String teacherName) {
        TeacherDTO dto = new TeacherDTO();
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacherName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un TeacherDTO con asignatura.
     *
     * @param teacherId ID del docente
     * @param teacherName Nombre del docente
     * @param subjectId ID de la asignatura
     * @return TeacherDTO con asignatura asignada
     */
    public static TeacherDTO createWithSubject(int teacherId, String teacherName, int subjectId) {
        TeacherDTO dto = new TeacherDTO();
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacherName);
        dto.setSubjectId(subjectId);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un TeacherDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return TeacherDTO con valores por defecto
     */
    public static TeacherDTO empty() {
        TeacherDTO dto = new TeacherDTO();
        dto.setAvailabilitySummary("Sin disponibilidad");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Valida si el DTO tiene información de foto.
     *
     * @return true si tiene foto asignada
     */
    public boolean hasPhoto() {
        return photoData != null && photoData.length > 0;
    }

    /**
     * Valida si el docente tiene disponibilidad configurada.
     *
     * @return true si tiene disponibilidad asignada
     */
    public boolean hasAvailability() {
        return availabilitySummary != null &&
                !availabilitySummary.trim().isEmpty() &&
                !"Sin disponibilidad".equals(availabilitySummary.trim());
    }

    /**
     * Valida si el docente tiene una asignatura asignada.
     *
     * @return true si tiene asignatura asignada
     */
    public boolean hasSubject() {
        return subjectId > 0;
    }

    /**
     * Obtiene una representación resumida del docente.
     * Formato: "ID: [teacherId] - [teacherName]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("ID: %d - %s", teacherId, teacherName != null ? teacherName : "Sin nombre");
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return teacherName != null && !teacherName.trim().isEmpty() &&
                teacherName.length() >= 5 && teacherName.length() <= 50 &&
                teacherName.matches("^[a-zA-ZÀ-ÿ\\s]+$");
    }
}