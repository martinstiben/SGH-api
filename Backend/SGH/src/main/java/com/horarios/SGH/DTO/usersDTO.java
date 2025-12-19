package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de información de usuarios del sistema SGH.
 * Implementa validaciones de negocio específicas para datos de usuario
 * y métodos de utilidad para gestión de fotos de perfil.
 *
 * Proporciona métodos Factory para crear usuarios
 * y validaciones de formato de archivos de imagen.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de información de usuarios")
public class usersDTO extends AbstractDTO {

    /**
     * Identificador único del usuario.
     */
    @Schema(description = "ID único del usuario", example = "1")
    private Long userId;

    /**
     * Nombre de usuario para autenticación.
     */
    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(max = 50, message = "El nombre de usuario no puede exceder los 50 caracteres")
    @Pattern(regexp = "^[a-z]*$", message = "El nombre de usuario solo puede contener letras minúsculas")
    @Schema(description = "Nombre de usuario", example = "juanperez")
    private String userName;

    /**
     * Contraseña del usuario (solo para creación/actualización).
     */
    @NotBlank(message = "La contraseña no puede estar vacía")
    @Schema(description = "Contraseña del usuario", example = "MiPass123")
    private String password;

    /**
     * Rol del usuario en el sistema.
     */
    @NotNull(message = "El rol no puede ser nulo")
    @Schema(description = "Rol del usuario", example = "ESTUDIANTE")
    private String role;

    /**
     * Datos binarios de la foto de perfil.
     */
    @Schema(description = "Datos binarios de la foto de perfil")
    private byte[] photoData;

    /**
     * Tipo de contenido de la foto (MIME type).
     */
    @Schema(description = "Tipo de contenido de la foto", example = "image/jpeg")
    private String photoContentType;

    /**
     * Nombre del archivo de la foto.
     */
    @Schema(description = "Nombre del archivo de la foto", example = "foto_perfil.jpg")
    private String photoFileName;

    /**
     * Timestamp de creación del usuario.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Método Factory para crear un usuario básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param userName nombre de usuario
     * @param password contraseña
     * @param role rol del usuario
     * @return usersDTO configurado
     */
    public static usersDTO create(String userName, String password, String role) {
        usersDTO dto = new usersDTO();
        dto.setUserName(userName);
        dto.setPassword(password);
        dto.setRole(role);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un usuario con foto.
     *
     * @param userName nombre de usuario
     * @param password contraseña
     * @param role rol del usuario
     * @param photoData datos de la foto
     * @param photoContentType tipo de contenido
     * @param photoFileName nombre del archivo
     * @return usersDTO con foto configurada
     */
    public static usersDTO createWithPhoto(String userName, String password, String role,
                                         byte[] photoData, String photoContentType, String photoFileName) {
        usersDTO dto = create(userName, password, role);
        dto.setPhotoData(photoData);
        dto.setPhotoContentType(photoContentType);
        dto.setPhotoFileName(photoFileName);
        return dto;
    }

    /**
     * Método Factory para crear un usersDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return usersDTO con valores por defecto
     */
    public static usersDTO empty() {
        usersDTO dto = new usersDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el usuario tiene foto de perfil.
     *
     * @return true si tiene foto
     */
    public boolean hasPhoto() {
        return photoData != null && photoData.length > 0;
    }

    /**
     * Verifica si el tipo de contenido de la foto es válido.
     *
     * @return true si el tipo es válido
     */
    public boolean hasValidPhotoType() {
        if (photoContentType == null) {
            return false;
        }

        return photoContentType.equals("image/jpeg") ||
               photoContentType.equals("image/jpg") ||
               photoContentType.equals("image/png") ||
               photoContentType.equals("image/gif");
    }

    /**
     * Verifica si el rol es válido.
     *
     * @return true si el rol está en la lista de roles permitidos
     */
    public boolean hasValidRole() {
        if (role == null) {
            return false;
        }

        return role.equals("ESTUDIANTE") || role.equals("MAESTRO") ||
               role.equals("COORDINADOR") || role.equals("DIRECTOR_DE_AREA");
    }

    /**
     * Verifica si el nombre de usuario tiene formato válido.
     *
     * @return true si el formato es válido
     */
    public boolean hasValidUsernameFormat() {
        return userName != null && userName.matches("^[a-z]*$");
    }

    /**
     * Valida si el DTO tiene información básica completa.
     * Método de validación de negocio.
     *
     * @return true si tiene nombre, contraseña y rol válidos
     */
    public boolean isValid() {
        return userName != null && !userName.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               role != null && !role.trim().isEmpty() &&
               hasValidUsernameFormat() && hasValidRole();
    }

    /**
     * Obtiene una representación resumida del usuario.
     * Formato: "Usuario [userName] - Rol: [role] - Foto: [sí/no]"
     *
     * @return Representación resumida
     */
    public String getSummary() {
        return String.format("Usuario %s - Rol: %s - Foto: %s",
                userName != null ? userName : "Sin nombre",
                role != null ? role : "Sin rol",
                hasPhoto() ? "Sí" : "No");
    }

    // Getters y Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
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