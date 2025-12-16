package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity(name = "user_credentials")
@Data
public class UserCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Long credentialId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "El usuario es obligatorio")
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotNull(message = "El hash de la contraseña es obligatorio")
    @Size(min = 60, max = 255, message = "El hash de la contraseña debe tener entre 60 y 255 caracteres")
    private String passwordHash;

    @Column(name = "password_salt", length = 255)
    @Size(max = 255, message = "El salt debe tener máximo 255 caracteres")
    private String passwordSalt;

    @Column(name = "password_algorithm", length = 50)
    @Size(max = 50, message = "El algoritmo debe tener máximo 50 caracteres")
    private String passwordAlgorithm = "BCrypt";

    @Column(name = "password_changed_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public UserCredentials() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserCredentials(User user, String passwordHash) {
        this();
        this.user = user;
        this.passwordHash = passwordHash;
    }
}