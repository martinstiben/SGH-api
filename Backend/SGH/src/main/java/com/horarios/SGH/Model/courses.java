package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa un curso en el sistema SGH.
 * Un curso está asociado a un docente y materia específicos, y puede tener un director de grado.
 * Extiende AbstractEntity para funcionalidades comunes.
 *
 * Patrones de diseño aplicados:
 * - Template Method: Para validación y resúmenes
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name="courses")
public class courses extends AbstractEntity {

    /**
     * Identificador único del curso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="courseId")
    private int id;

    /**
     * Nombre del curso (ej: "1A", "2B").
     * Debe ser único y contener solo letras, números y espacios.
     */
    @Column(name="courseName", nullable=false, unique=true, length = 50)
    @NotNull(message = "El nombre del curso no puede ser nulo")
    @NotBlank(message = "El nombre del curso no puede estar vacío")
    @Size(min = 1, max = 50, message = "El nombre del curso debe tener entre 1 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "El nombre del curso solo puede contener letras, números y espacios")
    private String courseName;

    /**
     * Año académico al que pertenece el curso.
     */
    @Column(name="academic_year", nullable=false, length = 20)
    @NotNull(message = "El año académico es obligatorio")
    @Size(min = 4, max = 20, message = "El año académico debe tener entre 4 y 20 caracteres")
    private String academicYear;

    /**
     * Relación con el docente y materia que imparte el curso.
     * Esta relación es opcional, permitiendo cursos sin profesor asignado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_subject_id")
    private TeacherSubject teacherSubject;

    /**
     * Director de grado asignado al curso (opcional).
     * Representa al docente responsable del curso a nivel administrativo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_director_id")
    private teachers gradeDirector;

    /**
     * Constructor vacío requerido por JPA.
     */
    public courses() {}

    /**
     * Constructor con parámetros básicos para creación de cursos.
     *
     * @param id identificador único del curso
     * @param courseName nombre del curso (ej: "1A", "2B")
     */
    public courses(int id, String courseName) {
        this.id = id;
        this.courseName = courseName;
    }

    /**
     * Obtiene el identificador único del curso.
     *
     * @return ID del curso
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del curso.
     *
     * @param id ID del curso
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del curso.
     *
     * @return nombre del curso
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * Establece el nombre del curso.
     *
     * @param courseName nombre del curso
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * Obtiene el año académico al que pertenece el curso.
     *
     * @return año académico
     */
    public String getAcademicYear() {
        return academicYear;
    }

    /**
     * Establece el año académico al que pertenece el curso.
     *
     * @param academicYear año académico
     */
    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    /**
     * Obtiene la relación docente-materia asignada al curso.
     *
     * @return relación TeacherSubject asignada
     */
    public TeacherSubject getTeacherSubject() {
        return teacherSubject;
    }

    /**
     * Establece la relación docente-materia asignada al curso.
     *
     * @param teacherSubject relación TeacherSubject a asignar
     */
    public void setTeacherSubject(TeacherSubject teacherSubject) {
        this.teacherSubject = teacherSubject;
    }

    /**
     * Obtiene el director de grado asignado al curso.
     *
     * @return docente director de grado
     */
    public teachers getGradeDirector() {
        return gradeDirector;
    }

    /**
     * Establece el director de grado asignado al curso.
     *
     * @param gradeDirector docente director de grado
     */
    public void setGradeDirector(teachers gradeDirector) {
        this.gradeDirector = gradeDirector;
    }

    /**
     * Valida la entidad antes de persistirla.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del curso no puede estar vacío");
        }
        if (academicYear == null || academicYear.trim().isEmpty()) {
            throw new IllegalArgumentException("El año académico es obligatorio");
        }
    }

    /**
     * Obtiene una representación resumida de la entidad.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Curso: " + courseName + " (" + academicYear + ")";
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
     * Representación en string de la entidad.
     *
     * @return string con información básica del curso
     */
    @Override
    public String toString() {
        return "courses{" +
                "id=" + id +
                ", courseName='" + courseName + '\'' +
                ", academicYear='" + academicYear + '\'' +
                ", gradeDirector=" + (gradeDirector != null ? gradeDirector.getTeacherName() : "null") +
                '}';
    }
}