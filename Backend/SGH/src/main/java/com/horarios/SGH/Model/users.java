                                                                                    package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity(name = "users")
public class users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NotNull(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    @NotNull(message = "El email es obligatorio")
    @Size(max = 254, message = "El email debe tener máximo 254 caracteres")
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotNull(message = "El apellido es obligatorio")
    @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
    private String lastName;

    @OneToOne
    @JoinColumn(name = "person_id", nullable = false)
    @NotNull(message = "La persona es obligatoria")
    private People person;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private courses course;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 15)
    @NotNull(message = "El estado de la cuenta es obligatorio")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "last_password_change", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastPasswordChange;

    @Column(name = "password_never_expires", nullable = false)
    private boolean passwordNeverExpires = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime createdAt;

    // Constructor vacío
    public users() {
    }

    // Constructor con parámetros principales
    public users(People person, String passwordHash) {
        this.person = person;
        this.createdAt = java.time.LocalDateTime.now();
    }

    // Getters y setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public People getPerson() {
        return person;
    }

    public void setPerson(People person) {
        this.person = person;
    }

    public courses getCourse() {
        return course;
    }

    public void setCourse(courses course) {
        this.course = course;
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

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public boolean isPasswordNeverExpires() {
        return passwordNeverExpires;
    }

    public void setPasswordNeverExpires(boolean passwordNeverExpires) {
        this.passwordNeverExpires = passwordNeverExpires;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return person != null ? person.getPersonalEmail() : null;
    }

    public void setUserName(String userName) {
        // Este método puede no ser necesario, pero lo incluimos por compatibilidad
    }

    public boolean isVerified() {
        return isVerified;
    }

}