package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity(name="subjects")
public class subjects {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="subjectId")
    private int id;

    @Column(name="subjectName", nullable=false, unique=true)
    @NotNull(message = "El nombre de la materia no puede ser nulo")
    @NotBlank(message = "El nombre de la materia no puede estar vacío")
    @Size(min = 4, max = 20, message = "El nombre de la materia debe tener entre 4 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre de la materia solo puede contener letras y espacios")
    private String subjectName;

    // Relaciones normalizadas
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeacherSubject> teacherSubjects;
    
    @OneToMany(mappedBy = "subjectId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<schedule> schedules;

    public subjects() {}

    public subjects(int id, String subjectName) {
        this.id = id;
        this.subjectName = subjectName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    // Getters y setters para relaciones
    public List<TeacherSubject> getTeacherSubjects() {
        return teacherSubjects;
    }

    public void setTeacherSubjects(List<TeacherSubject> teacherSubjects) {
        this.teacherSubjects = teacherSubjects;
    }

    public List<schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<schedule> schedules) {
        this.schedules = schedules;
    }
}