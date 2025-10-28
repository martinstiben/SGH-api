package com.horarios.SGH.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TeacherDTO {

    @NotBlank(message = "El nombre del profesor no puede estar vacío")
    @Size(min = 5, max = 50, message = "El nombre del profesor debe tener entre 5 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre del profesor solo puede contener letras y espacios")
    private String teacherName;

    // Para compatibilidad con el servicio existente
    private int teacherId;
    private int subjectId;

    // Información de disponibilidad
    private String availabilitySummary;

    // Información de foto
    private byte[] photoData;
    private String photoContentType;
    private String photoFileName;

    public TeacherDTO() {}

    public TeacherDTO(String teacherName) {
        this.teacherName = teacherName;
    }

    // Getters y Setters
    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }


    // Métodos para compatibilidad con el servicio existente
    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
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
}