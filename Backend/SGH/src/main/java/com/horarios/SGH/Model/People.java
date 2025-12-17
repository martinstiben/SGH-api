package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity(name = "people")
@Data
public class People {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id")
    private int personId;

    @Column(name = "full_name", nullable = false, length = 100)
    @NotNull(message = "El nombre completo es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre completo debe tener entre 1 y 100 caracteres")
    private String fullName;

    @Column(name = "personal_email", nullable = true, length = 254)
    @Size(max = 254, message = "El email personal debe tener máximo 254 caracteres")
    private String personalEmail;

    @Column(name = "photo_file_name", length = 255)
    private String photoFileName;

    @Column(name = "photo_content_type", length = 100)
    private String photoContentType;

    @Column(name = "photo_data", columnDefinition = "MEDIUMBLOB")
    @Lob
    private byte[] photoData;

    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private users user;

    // Constructor vacío
    public People() {}

    // Constructor con parámetros principales
    public People(String fullName, String personalEmail) {
        this.fullName = fullName;
        this.personalEmail = personalEmail;
    }

    public String getEmail() {
        return personalEmail;
    }

    public void setEmail(String email) {
        this.personalEmail = email;
    }
}