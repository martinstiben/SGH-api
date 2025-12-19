package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de credenciales de usuario del sistema SGH.
 * Implementa validaciones de negocio específicas para hashes de contraseña
 * y métodos de utilidad para gestión de seguridad de credenciales.
 *
 * Proporciona métodos Factory para crear credenciales
 * y validaciones de formato de hash de contraseña.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de credenciales de usuario")
public class UserCredentialsDTO extends AbstractDTO {

    /**
     * Identificador único de las credenciales.
     */
    @Schema(description = "ID único de las credenciales", example = "1")
    private Long credentialId;

    /**
     * Hash de la contraseña usando BCrypt.
     */
    @NotNull(message = "El hash de la contraseña es obligatorio")
    @Size(min = 60, max = 255, message = "El hash de la contraseña debe tener entre 60 y 255 caracteres")
    @Schema(description = "Hash de la contraseña con BCrypt", example = "$2a$10$...")
    private String passwordHash;

    /**
     * Salt usado para el hash de la contraseña.
     */
    @Size(max = 255, message = "El salt debe tener máximo 255 caracteres")
    @Schema(description = "Salt usado para el hash", example = "abc123...")
    private String passwordSalt;

    /**
     * Algoritmo usado para hashear la contraseña.
     */
    @Size(max = 50, message = "El algoritmo debe tener máximo 50 caracteres")
    @Schema(description = "Algoritmo de hash usado", example = "BCrypt")
    private String passwordAlgorithm = "BCrypt";

    /**
     * Timestamp del último cambio de contraseña.
     */
    @Schema(description = "Fecha del último cambio de contraseña", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime passwordChangedAt;

    /**
     * Timestamp de creación de las credenciales.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public UserCredentialsDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param passwordHash hash de la contraseña
     */
    public UserCredentialsDTO(String passwordHash) {
        super();
        this.passwordHash = passwordHash;
        this.passwordAlgorithm = "BCrypt";
        this.passwordChangedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear credenciales básicas.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param passwordHash hash de la contraseña
     * @return UserCredentialsDTO configurado
     */
    public static UserCredentialsDTO create(String passwordHash) {
        UserCredentialsDTO dto = new UserCredentialsDTO();
        dto.setPasswordHash(passwordHash);
        dto.setPasswordAlgorithm("BCrypt");
        dto.setPasswordChangedAt(LocalDateTime.now());
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear credenciales con salt.
     *
     * @param passwordHash hash de la contraseña
     * @param passwordSalt salt usado
     * @return UserCredentialsDTO configurado
     */
    public static UserCredentialsDTO createWithSalt(String passwordHash, String passwordSalt) {
        UserCredentialsDTO dto = create(passwordHash);
        dto.setPasswordSalt(passwordSalt);
        return dto;
    }

    /**
     * Método Factory para crear un UserCredentialsDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return UserCredentialsDTO con valores por defecto
     */
    public static UserCredentialsDTO empty() {
        UserCredentialsDTO dto = new UserCredentialsDTO();
        dto.setPasswordAlgorithm("BCrypt");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el hash de contraseña tiene formato BCrypt válido.
     * Una contraseña BCrypt válida comienza con $2a$, $2b$ o $2y$.
     *
     * @return true si tiene formato BCrypt válido
     */
    public boolean hasValidBCryptFormat() {
        return passwordHash != null &&
               (passwordHash.startsWith("$2a$") ||
                passwordHash.startsWith("$2b$") ||
                passwordHash.startsWith("$2y$"));
    }

    /**
     * Verifica si la contraseña necesita ser cambiada.
     * Considera que necesita cambio si tiene más de 90 días.
     *
     * @return true si necesita cambio
     */
    public boolean needsPasswordChange() {
        if (passwordChangedAt == null) {
            return true; // Si no hay fecha de cambio, asumir que necesita cambio
        }

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        return passwordChangedAt.isBefore(ninetyDaysAgo);
    }

    /**
     * Verifica si las credenciales están actualizadas recientemente.
     *
     * @return true si fueron actualizadas en las últimas 24 horas
     */
    public boolean isRecentlyUpdated() {
        if (updatedAt == null) {
            return false;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return updatedAt.isAfter(oneDayAgo);
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return passwordHash != null && !passwordHash.trim().isEmpty() &&
               hasValidBCryptFormat() &&
               passwordAlgorithm != null && !passwordAlgorithm.trim().isEmpty();
    }

    /**
     * Obtiene una representación segura de las credenciales (sin mostrar el hash).
     * Formato: "Credenciales ID [credentialId] - Algoritmo: [passwordAlgorithm] - Creadas: [createdAt]"
     *
     * @return Representación segura
     */
    @Override
    public String getSummary() {
        return String.format("Credenciales ID %d - Algoritmo: %s - Creadas: %s",
                credentialId != null ? credentialId : 0,
                passwordAlgorithm != null ? passwordAlgorithm : "Desconocido",
                createdAt != null ? createdAt.toString() : "Fecha desconocida");
    }

    /**
     * Obtiene una representación segura de las credenciales (sin mostrar el hash).
     * Formato: "Credenciales ID [credentialId] - Algoritmo: [passwordAlgorithm] - Creadas: [createdAt]"
     *
     * @return Representación segura
     */
    public String getSecureSummary() {
        return String.format("Credenciales ID %d - Algoritmo: %s - Creadas: %s",
                credentialId != null ? credentialId : 0,
                passwordAlgorithm != null ? passwordAlgorithm : "Desconocido",
                createdAt != null ? createdAt.toString() : "Fecha desconocida");
    }

    // Getters y Setters
    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    public void setPasswordAlgorithm(String passwordAlgorithm) {
        this.passwordAlgorithm = passwordAlgorithm;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}