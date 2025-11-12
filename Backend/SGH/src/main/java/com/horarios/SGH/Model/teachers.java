package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
}