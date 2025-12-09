package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity(name="teachers")
public class teachers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="teacherId")
    private int id;

    @Column(name="teacherName", length = 100, nullable=false)
    @NotBlank(message = "El nombre del profesor no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre del profesor debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre del profesor solo puede contener letras y espacios")
    private String teacherName;

    @Column(name="photoData", columnDefinition = "LONGBLOB")
    @Lob
    private byte[] photoData;

    @Column(name="photoContentType", length = 100)
    private String photoContentType;

    @Column(name="photoFileName", length = 255)
    private String photoFileName;

    // Relaciones normalizadas
    @OneToMany(mappedBy = "teacherId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<schedule> schedules;
    
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeacherSubject> teacherSubjects;
    
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeacherAvailability> availabilities;
    
    // Relación inversa para director de grado
    @OneToMany(mappedBy = "gradeDirector", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<courses> directedCourses;

    public teachers() {}

    public teachers(int id, String teacherName) {
        this.id = id;
        this.teacherName = teacherName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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

    // Getters y setters para relaciones
    public List<schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<schedule> schedules) {
        this.schedules = schedules;
    }

    public List<TeacherSubject> getTeacherSubjects() {
        return teacherSubjects;
    }

    public void setTeacherSubjects(List<TeacherSubject> teacherSubjects) {
        this.teacherSubjects = teacherSubjects;
    }

    public List<TeacherAvailability> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<TeacherAvailability> availabilities) {
        this.availabilities = availabilities;
    }

    public List<courses> getDirectedCourses() {
        return directedCourses;
    }

    public void setDirectedCourses(List<courses> directedCourses) {
        this.directedCourses = directedCourses;
    }
}