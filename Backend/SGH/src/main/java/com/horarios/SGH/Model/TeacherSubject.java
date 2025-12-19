package com.horarios.SGH.Model;

import jakarta.persistence.*;

/**
 * Entidad de relación muchos-a-muchos entre docentes y materias.
 * Representa la asignación de una materia específica a un docente,
 * permitiendo que un docente imparta múltiples materias y que una materia
 * sea impartida por múltiples docentes.
 *
 * Esta entidad es fundamental para el sistema de horarios ya que establece
 * las combinaciones válidas docente-materia que pueden ser asignadas a cursos.
 *
 * @author Sistema SGH
 * @version 1.0
 */
/**
 * Entidad de relación muchos-a-muchos entre docentes y materias.
 * Representa la asignación de una materia específica a un docente,
 * permitiendo que un docente imparta múltiples materias y que una materia
 * sea impartida por múltiples docentes.
 *
 * Esta entidad es fundamental para el sistema de horarios ya que establece
 * las combinaciones válidas docente-materia que pueden ser asignadas a cursos.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar relaciones docente-materia
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
@Entity
@Table(name = "TeacherSubject",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id","subject_id"}))
public class TeacherSubject extends AbstractEntity {

    /**
     * Identificador único de la relación docente-materia.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_subject_id")
    private Integer id;

    /**
     * Docente que imparte la materia.
     * Relación obligatoria con la entidad teachers.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private teachers teacher;

    /**
     * Materia que es impartida por el docente.
     * Relación obligatoria con la entidad subjects.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private subjects subject;

    /**
     * Constructor vacío requerido por JPA.
     */
    public TeacherSubject() {
        super();
    }

    /**
     * Constructor con parámetros para crear una relación docente-materia.
     *
     * @param teacher docente que imparte la materia
     * @param subject materia impartida
     */
    public TeacherSubject(teachers teacher, subjects subject) {
        this.teacher = teacher;
        this.subject = subject;
    }

    /**
     * Obtiene el identificador único de la relación.
     *
     * @return ID de la relación docente-materia
     */
    public Integer getId() { return id; }

    /**
     * Establece el identificador único de la relación.
     *
     * @param id ID de la relación docente-materia
     */
    public void setId(Integer id) { this.id = id; }

    /**
     * Obtiene el docente de la relación.
     *
     * @return docente que imparte la materia
     */
    public teachers getTeacher() { return teacher; }

    /**
     * Establece el docente de la relación.
     *
     * @param teacher docente que imparte la materia
     */
    public void setTeacher(teachers teacher) { this.teacher = teacher; }

    /**
     * Obtiene la materia de la relación.
     *
     * @return materia impartida por el docente
     */
    public subjects getSubject() { return subject; }

    /**
     * Establece la materia de la relación.
     *
     * @param subject materia impartida por el docente
     */
    public void setSubject(subjects subject) { this.subject = subject; }

    /**
     * Valida si la entidad tiene información básica completa.
     * Método de validación de negocio.
     */
    @Override
    public void validate() {
        if (teacher == null) {
            throw new IllegalArgumentException("El docente no puede ser nulo");
        }
        if (subject == null) {
            throw new IllegalArgumentException("La materia no puede ser nula");
        }
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     * Una entidad es nueva si no tiene ID asignado.
     *
     * @return true si es una nueva entidad
     */
    @Override
    public boolean isNew() {
        return id == null;
    }

    /**
     * Obtiene una representación resumida de la relación.
     * Formato: "Relación [id] - [teacherName] -> [subjectName]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String teacherName = teacher != null ? teacher.getTeacherName() : "Sin docente";
        String subjectName = subject != null ? subject.getSubjectName() : "Sin materia";
        return String.format("Relación %d - %s -> %s",
                id != null ? id : 0,
                teacherName,
                subjectName);
    }

    /**
     * Representación en string de la relación docente-materia.
     *
     * @return string con información de la relación
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TeacherSubject{")
          .append("id=").append(id)
          .append(", teacher=").append(teacher != null ? teacher.getTeacherName() : "null")
          .append(", subject=").append(subject != null ? subject.getSubjectName() : "null")
          .append("} ");
        
        // Agregar información de timestamps si están disponibles
        if (getCreatedAt() != null) {
            sb.append("Creado: ").append(getCreatedAt());
        }
        
        return sb.toString();
    }

    /**
     * Compara dos objetos TeacherSubject por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TeacherSubject that = (TeacherSubject) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (teacher != null ? !teacher.equals(that.teacher) : that.teacher != null) return false;
        return subject != null ? subject.equals(that.subject) : that.subject == null;
    }

    /**
     * Genera el código hash del objeto.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (teacher != null ? teacher.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        return result;
    }
}