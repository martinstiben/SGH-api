package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity(name="courses")
public class courses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="courseId")
    private int id;

    @Column(name="courseName", nullable=false, unique=true)
    @NotNull(message = "El nombre del curso no puede ser nulo")
    @NotBlank(message = "El nombre del curso no puede estar vacío")
    @Size(min = 1, max = 50, message = "El nombre del curso debe tener entre 1 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El nombre del curso solo puede contener letras y números")
    private String courseName;

    // Director de grado (uno por curso, opcional)
    @ManyToOne
    @JoinColumn(name = "grade_director_id")
    private teachers gradeDirector;

    // Relaciones normalizadas
    @OneToMany(mappedBy = "courseId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<schedule> schedules;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<users> users;

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

    public teachers getGradeDirector() {
        return gradeDirector;
    }

    public void setGradeDirector(teachers gradeDirector) {
        this.gradeDirector = gradeDirector;
    }

    // Getters y setters para relaciones
    public List<schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<schedule> schedules) {
        this.schedules = schedules;
    }

    public List<users> getUsers() {
        return users;
    }

    public void setUsers(List<users> users) {
        this.users = users;
    }
}