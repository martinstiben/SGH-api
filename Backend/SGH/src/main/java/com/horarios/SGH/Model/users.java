package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity(name = "users")
@Data
public class users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @OneToOne
    @JoinColumn(name = "person_id", nullable = false)
    @NotNull(message = "La persona es obligatoria")
    private People person;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull(message = "El rol es obligatorio")
    private Roles role;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotNull(message = "El hash de la contraseña es obligatorio")
    @Size(min = 1, max = 255, message = "El hash de la contraseña debe tener entre 1 y 255 caracteres")
    private String passwordHash;

    @Column(name = "verification_code", length = 255)
    @Size(max = 255, message = "El código de verificación debe tener máximo 255 caracteres")
    private String verificationCode;

    @Column(name = "code_expiration", columnDefinition = "DATETIME(6)")
    private java.time.LocalDateTime codeExpiration;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 15)
    @NotNull(message = "El estado de la cuenta es obligatorio")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime createdAt;

    // Constructor vacío
    public users() {
    }

    // Constructor con parámetros principales
    public users(People person, Roles role, String passwordHash) {
        this.person = person;
        this.role = role;
        this.passwordHash = passwordHash;
        this.createdAt = java.time.LocalDateTime.now();
    }

    // Getters y setters generados por Lombok (@Data)
}