package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO para solicitud de autenticación/login del sistema SGH.
 * Implementa el patrón Builder para construcción flexible y validaciones
 * de negocio específicas para credenciales de acceso.
 *
 * Proporciona métodos de utilidad para validación y sanitización
 * de datos de autenticación con medidas de seguridad básicas.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para solicitud de login")
public class LoginRequestDTO extends AbstractDTO {

    /**
     * Dirección de correo electrónico del usuario.
     * Debe ser una dirección válida y única en el sistema.
     */
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@example.com", required = true)
    private String email;

    /**
     * Contraseña del usuario.
     * Debe cumplir con políticas de seguridad: minúscula, mayúscula y número.
     */
    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
    @Schema(description = "Contraseña (debe contener minúscula, mayúscula y número)", example = "Password123", required = true)
    private String password;

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Método Factory para crear un LoginRequestDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param email Correo electrónico
     * @param password Contraseña
     * @return LoginRequestDTO configurado
     */
    public static LoginRequestDTO create(String email, String password) {
        return new LoginRequestDTO(email, password);
    }

    /**
     * Método Factory para crear un LoginRequestDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return LoginRequestDTO con valores por defecto
     */
    public static LoginRequestDTO empty() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Obtiene el dominio del correo electrónico.
     * Ejemplo: "usuario@example.com" -> "example.com"
     *
     * @return Dominio del email, o null si no es válido
     */
    public String getEmailDomain() {
        if (email == null || !email.contains("@")) {
            return null;
        }
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }

    /**
     * Verifica si el email pertenece a un dominio educativo común.
     *
     * @return true si es un dominio educativo (.edu, .ac, etc.)
     */
    public boolean isEducationalEmail() {
        String domain = getEmailDomain();
        if (domain == null) {
            return false;
        }
        return domain.endsWith(".edu") ||
               domain.endsWith(".ac") ||
               domain.contains("school") ||
               domain.contains("university") ||
               domain.contains("college");
    }

    /**
     * Valida la fortaleza de la contraseña.
     * Método adicional de validación más allá de las anotaciones.
     *
     * @return Nivel de fortaleza: WEAK, MEDIUM, STRONG
     */
    public PasswordStrength getPasswordStrength() {
        if (password == null || password.length() < 6) {
            return PasswordStrength.WEAK;
        }

        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        int length = password.length();

        if (hasLower && hasUpper && hasDigit && hasSpecial && length >= 12) {
            return PasswordStrength.STRONG;
        } else if (hasLower && hasUpper && hasDigit && length >= 8) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }

    /**
     * Constructor por defecto.
     */
    public LoginRequestDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param email Correo electrónico
     * @param password Contraseña
     */
    public LoginRequestDTO(String email, String password) {
        super();
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") &&
               password != null && !password.trim().isEmpty() &&
               password.length() >= 6 && password.length() <= 100 &&
               password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
    }

    /**
     * Obtiene una representación resumida de la solicitud de login.
     * Formato: "Login para [email]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Login para %s",
                email != null ? email : "Sin email");
    }

    /**
     * Sanitiza los datos de entrada eliminando espacios extra.
     * Método de utilidad para normalización.
     */
    public void sanitize() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (password != null) {
            password = password.trim();
        }
    }

    /**
     * Enum para niveles de fortaleza de contraseña.
     */
    public enum PasswordStrength {
        WEAK("Débil"),
        MEDIUM("Media"),
        STRONG("Fuerte");

        private final String description;

        PasswordStrength(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}