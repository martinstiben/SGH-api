package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.horarios.SGH.Model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para información de estudiantes en un curso específico.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones específicas para datos de estudiantes matriculados.
 *
 * Proporciona información resumida de estudiantes con estado de cuenta
 * y verificación para gestión académica.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar estudiantes en cursos
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Implementado a través de AbstractDTO
 * - Factory Method: Para creación de instancias
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para información de estudiantes en un curso")
public class CourseStudentDTO extends AbstractDTO {

    /**
     * Identificador único del estudiante en el sistema.
     * Corresponde al ID del usuario.
     */
    @NotNull(message = "El ID del usuario es obligatorio")
    @Schema(description = "ID único del estudiante", example = "1")
    private Long userId;

    /**
     * Nombre completo del estudiante.
     * Incluye nombre y apellidos.
     */
    @NotNull(message = "El nombre completo es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre completo debe tener entre 1 y 100 caracteres")
    @Schema(description = "Nombre completo del estudiante", example = "Juan Pérez García")
    private String fullName;

    /**
     * Dirección de correo electrónico del estudiante.
     * Debe ser única y válida.
     */
    @NotNull(message = "El email es obligatorio")
    @Size(min = 1, max = 150, message = "El email debe tener entre 1 y 150 caracteres")
    @Schema(description = "Correo electrónico del estudiante", example = "juan.perez@universidad.edu")
    private String email;

    /**
     * Nombre del rol del usuario en el sistema.
     * Para estudiantes siempre debería ser "ESTUDIANTE".
     */
    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 1, max = 50, message = "El nombre del rol debe tener entre 1 y 50 caracteres")
    @Schema(description = "Rol del usuario", example = "ESTUDIANTE", allowableValues = {"ESTUDIANTE"})
    private String roleName;

    /**
     * Estado actual de la cuenta del estudiante.
     * Indica si está activa, suspendida, etc.
     */
    @NotNull(message = "El estado de la cuenta es obligatorio")
    @Schema(description = "Estado de la cuenta del estudiante", example = "ACTIVE")
    private AccountStatus accountStatus;

    /**
     * Indica si la cuenta del estudiante ha sido verificada.
     * Las cuentas verificadas tienen acceso completo al sistema.
     */
    @Schema(description = "Indica si la cuenta está verificada", example = "true")
    private boolean isVerified;

    /**
     * Timestamp de la última actividad del estudiante.
     */
    @Schema(description = "Fecha de última actividad", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lastActivity;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Método Factory para crear un CourseStudentDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param userId ID del estudiante
     * @param fullName Nombre completo
     * @param email Correo electrónico
     * @param roleName Nombre del rol
     * @param accountStatus Estado de la cuenta
     * @param isVerified Si está verificado
     * @return CourseStudentDTO configurado
     */
    public static CourseStudentDTO create(Long userId, String fullName, String email,
                                         String roleName, AccountStatus accountStatus, boolean isVerified) {
        CourseStudentDTO dto = new CourseStudentDTO();
        dto.setUserId(userId);
        dto.setFullName(fullName);
        dto.setEmail(email);
        dto.setRoleName(roleName);
        dto.setAccountStatus(accountStatus);
        dto.setVerified(isVerified);
        dto.setLastActivity(LocalDateTime.now());
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un CourseStudentDTO vacío.
     * Útil para formularios o respuestas vacías.
     *
     * @return CourseStudentDTO con valores por defecto
     */
    public static CourseStudentDTO empty() {
        CourseStudentDTO dto = new CourseStudentDTO();
        dto.setAccountStatus(AccountStatus.ACTIVE);
        dto.setVerified(false);
        return dto;
    }

    // Getters y Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Verifica si el estudiante tiene una cuenta activa.
     *
     * @return true si la cuenta está activa
     */
    public boolean hasActiveAccount() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    /**
     * Verifica si el estudiante puede acceder al sistema.
     * Combinación de estado activo y verificación.
     *
     * @return true si puede acceder
     */
    public boolean canAccessSystem() {
        return hasActiveAccount() && isVerified;
    }

    /**
     * Obtiene el nombre corto del estudiante (primer nombre).
     *
     * @return Primer nombre o nombre completo si no hay espacios
     */
    public String getShortName() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts[0];
    }

    /**
     * Obtiene las iniciales del estudiante.
     * Ejemplo: "Juan Pérez García" -> "JPG"
     *
     * @return Iniciales en mayúsculas
     */
    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }

        String[] words = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(word.charAt(0));
            }
        }

        return initials.toString().toUpperCase();
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return userId != null && userId > 0 &&
                fullName != null && !fullName.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                roleName != null && !roleName.trim().isEmpty() &&
                accountStatus != null;
    }

    /**
     * Obtiene una representación resumida del estudiante.
     * Formato: "[fullName] ([email]) - [accountStatus]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s (%s) - %s",
                fullName != null ? fullName : "Sin nombre",
                email != null ? email : "Sin email",
                accountStatus != null ? accountStatus : "Sin estado");
    }
}