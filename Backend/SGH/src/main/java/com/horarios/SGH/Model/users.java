package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity(name = "users")
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

    @ManyToOne
    @JoinColumn(name = "course_id")
    private courses course;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotNull(message = "El hash de la contraseña es obligatorio")
    @Size(min = 1, max = 255, message = "El hash de la contraseña debe tener entre 1 y 255 caracteres")
    private String passwordHash;

    @Column(name = "verification_code", length = 255)
    @Size(max = 255, message = "El código de verificación debe tener máximo 255 caracteres")
    private String verificationCode;

    @Column(name = "code_expiration", columnDefinition = "DATETIME(6)")
    private java.time.LocalDateTime codeExpiration;

    @Column(name = "password_reset_code", length = 255)
    @Size(max = 255, message = "El código de reset debe tener máximo 255 caracteres")
    private String passwordResetCode;

    @Column(name = "password_reset_expiration", columnDefinition = "DATETIME(6)")
    private java.time.LocalDateTime passwordResetExpiration;

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

    // Getters y setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public People getPerson() {
        return person;
    }

    public void setPerson(People person) {
        this.person = person;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public courses getCourse() {
        return course;
    }

    public void setCourse(courses course) {
        this.course = course;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public java.time.LocalDateTime getCodeExpiration() {
        return codeExpiration;
    }

    public void setCodeExpiration(java.time.LocalDateTime codeExpiration) {
        this.codeExpiration = codeExpiration;
    }

    public String getPasswordResetCode() {
        return passwordResetCode;
    }

    public void setPasswordResetCode(String passwordResetCode) {
        this.passwordResetCode = passwordResetCode;
    }

    public java.time.LocalDateTime getPasswordResetExpiration() {
        return passwordResetExpiration;
    }

    public void setPasswordResetExpiration(java.time.LocalDateTime passwordResetExpiration) {
        this.passwordResetExpiration = passwordResetExpiration;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return person != null ? person.getEmail() : null;
    }

    public void setUserName(String userName) {
        // Este método puede no ser necesario, pero lo incluimos por compatibilidad
    }
}