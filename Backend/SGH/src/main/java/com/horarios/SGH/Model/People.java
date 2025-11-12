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

    @Column(name = "email", nullable = false, unique = true, length = 150)
    @NotNull(message = "El email es obligatorio")
    @Size(min = 1, max = 150, message = "El email debe tener entre 1 y 150 caracteres")
    private String email;

    @Column(name = "photo_file_name", length = 255)
    private String photoFileName;

    @Column(name = "photo_content_type", length = 100)
    private String photoContentType;

    @Column(name = "photo_data", columnDefinition = "LONGBLOB")
    @Lob
    private byte[] photoData;

    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private users user;

    // Constructor vacío
    public People() {}

    // Constructor con parámetros principales
    public People(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }
}