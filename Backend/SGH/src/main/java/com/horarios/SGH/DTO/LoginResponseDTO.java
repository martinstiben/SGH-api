package com.horarios.SGH.DTO;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de login exitoso.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con información de token JWT y datos básicos de usuario.
 *
 * Proporciona métodos de utilidad para validación y representación
 * de respuestas de autenticación exitosa.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar respuestas de login
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
public class LoginResponseDTO extends AbstractDTO {

    private String token;
    private Long userId;
    private String email;
    private String name;

    /**
     * Constructor por defecto.
     */
    public LoginResponseDTO() {
        super();
    }

    /**
     * Constructor con token.
     *
     * @param token Token JWT generado
     */
    public LoginResponseDTO(String token) {
        super();
        this.token = token;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor completo.
     *
     * @param token Token JWT generado
     * @param userId ID del usuario
     * @param email Email del usuario
     * @param name Nombre del usuario
     */
    public LoginResponseDTO(String token, Long userId, String email, String name) {
        super();
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una respuesta de login básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param token Token JWT
     * @return LoginResponseDTO configurado
     */
    public static LoginResponseDTO create(String token) {
        return new LoginResponseDTO(token);
    }

    /**
     * Método Factory para crear una respuesta de login completa.
     *
     * @param token Token JWT
     * @param userId ID del usuario
     * @param email Email del usuario
     * @param name Nombre del usuario
     * @return LoginResponseDTO configurado
     */
    public static LoginResponseDTO createComplete(String token, Long userId, String email, String name) {
        return new LoginResponseDTO(token, userId, email, name);
    }

    /**
     * Método Factory para crear un LoginResponseDTO vacío.
     *
     * @return LoginResponseDTO con valores por defecto
     */
    public static LoginResponseDTO empty() {
        LoginResponseDTO dto = new LoginResponseDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    // Getters y setters

    /**
     * Obtiene el token JWT.
     *
     * @return Token de autenticación
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token JWT.
     *
     * @param token Token de autenticación
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Obtiene el ID del usuario.
     *
     * @return ID del usuario
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Establece el ID del usuario.
     *
     * @param userId ID del usuario
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Obtiene el email del usuario.
     *
     * @return Email del usuario
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el email del usuario.
     *
     * @param email Email del usuario
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene el nombre del usuario.
     *
     * @return Nombre completo del usuario
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre del usuario.
     *
     * @param name Nombre completo del usuario
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Valida si el DTO tiene información básica válida.
     * Método de validación de negocio.
     *
     * @return true si tiene token y al menos un campo de usuario
     */
    @Override
    public boolean isValid() {
        return token != null && !token.trim().isEmpty() &&
               (userId != null || email != null || name != null);
    }

    /**
     * Obtiene una representación resumida de la respuesta de login.
     * Formato: "Login exitoso - Usuario: [name] ([email])"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Login exitoso - Usuario: %s (%s)",
                name != null ? name : "Sin nombre",
                email != null ? email : "Sin email");
    }
}