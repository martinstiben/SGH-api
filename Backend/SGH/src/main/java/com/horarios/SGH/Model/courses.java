package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity(name="courses")
public class courses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="courseId")
    private int id;

    @Column(name="courseName", nullable=false, unique=true, length = 50)
    @NotNull(message = "El nombre del curso no puede ser nulo")
    @NotBlank(message = "El nombre del curso no puede estar vacío")
    @Size(min = 1, max = 50, message = "El nombre del curso debe tener entre 1 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "El nombre del curso solo puede contener letras, números y espacios")
    private String courseName;

    @Column(name="academic_year", nullable=false, length = 20)
    @NotNull(message = "El año académico es obligatorio")
    @Size(min = 4, max = 20, message = "El año académico debe tener entre 4 y 20 caracteres")
    private String academicYear;

    // Docente+Materia que imparte el curso
    @ManyToOne
    @JoinColumn(name = "teacher_subject_id")
    private TeacherSubject teacherSubject;

    // Director de grado (uno por curso, opcional)
    @ManyToOne
    @JoinColumn(name = "grade_director_id")
    private teachers gradeDirector;

    public courses() {}

    public courses(int id, String courseName) {
        this.id = id;
        this.courseName = courseName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public TeacherSubject getTeacherSubject() {
        return teacherSubject;
    }

    public void setTeacherSubject(TeacherSubject teacherSubject) {
        this.teacherSubject = teacherSubject;
    }

    public teachers getGradeDirector() {
        return gradeDirector;
    }

    public void setGradeDirector(teachers gradeDirector) {
        this.gradeDirector = gradeDirector;
    }
}