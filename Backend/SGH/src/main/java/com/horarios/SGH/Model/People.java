package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa una persona en el sistema SGH.
 * Una persona puede estar asociada a un usuario del sistema,
 * conteniendo información básica como nombre completo, email personal
 * y foto de perfil.
 *
 * Esta entidad es la base para la gestión de usuarios, permitiendo
 * separar la información personal de la información de autenticación.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar personas
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractEntity
 *
 * Patrones de diseño aplicados:
 * - Template Method: Implementado a través de AbstractEntity
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name = "people")
public class People extends AbstractEntity {

    /**
     * Identificador único de la persona.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id")
    private int personId;

    /**
     * Nombre completo de la persona.
     * Incluye nombres y apellidos completos.
     */
    @Column(name = "full_name", nullable = false, length = 100)
    @NotNull(message = "El nombre completo es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre completo debe tener entre 1 y 100 caracteres")
    private String fullName;

    /**
     * Email personal de la persona (opcional).
     * Diferente del email institucional usado para login.
     */
    @Column(name = "personal_email", nullable = true, length = 254)
    @Size(max = 254, message = "El email personal debe tener máximo 254 caracteres")
    private String personalEmail;

    /**
     * Nombre del archivo de la foto de perfil.
     */
    @Column(name = "photo_file_name", length = 255)
    private String photoFileName;

    /**
     * Tipo de contenido de la foto (MIME type).
     */
    @Column(name = "photo_content_type", length = 100)
    private String photoContentType;

    /**
     * Datos binarios de la foto de perfil.
     */
    @Column(name = "photo_data", columnDefinition = "MEDIUMBLOB")
    @Lob
    private byte[] photoData;

    /**
     * Usuario asociado a esta persona.
     * Relación uno-a-uno con la entidad User.
     */
    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public People() {
        super();
    }

    /**
     * Constructor con parámetros principales para crear una persona.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param fullName nombre completo de la persona
     * @param personalEmail email personal (opcional)
     */
    public People(String fullName, String personalEmail) {
        super();
        this.fullName = fullName;
        this.personalEmail = personalEmail;
    }

    /**
     * Obtiene el identificador único de la persona.
     *
     * @return ID de la persona
     */
    public int getPersonId() {
        return personId;
    }

    /**
     * Establece el identificador único de la persona.
     *
     * @param personId ID de la persona
     */
    public void setPersonId(int personId) {
        this.personId = personId;
    }

    /**
     * Obtiene el nombre completo de la persona.
     *
     * @return nombre completo
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Establece el nombre completo de la persona.
     *
     * @param fullName nombre completo
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Obtiene el email personal de la persona.
     *
     * @return email personal
     */
    public String getPersonalEmail() {
        return personalEmail;
    }

    /**
     * Establece el email personal de la persona.
     *
     * @param personalEmail email personal
     */
    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    /**
     * Alias para getPersonalEmail() para compatibilidad.
     *
     * @return email personal
     */
    public String getEmail() {
        return personalEmail;
    }

    /**
     * Alias para setPersonalEmail() para compatibilidad.
     *
     * @param email email personal
     */
    public void setEmail(String email) {
        this.personalEmail = email;
    }

    /**
     * Obtiene el nombre del archivo de la foto de perfil.
     *
     * @return nombre del archivo de foto
     */
    public String getPhotoFileName() {
        return photoFileName;
    }

    /**
     * Establece el nombre del archivo de la foto de perfil.
     *
     * @param photoFileName nombre del archivo de foto
     */
    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    /**
     * Obtiene el tipo de contenido de la foto de perfil.
     *
     * @return tipo MIME de la foto
     */
    public String getPhotoContentType() {
        return photoContentType;
    }

    /**
     * Establece el tipo de contenido de la foto de perfil.
     *
     * @param photoContentType tipo MIME de la foto
     */
    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    /**
     * Obtiene los datos binarios de la foto de perfil.
     *
     * @return datos binarios de la foto
     */
    public byte[] getPhotoData() {
        return photoData;
    }

    /**
     * Establece los datos binarios de la foto de perfil.
     *
     * @param photoData datos binarios de la foto
     */
    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    /**
     * Obtiene el usuario asociado a esta persona.
     *
     * @return usuario asociado
     */
    public User getUser() {
        return user;
    }

    /**
     * Establece el usuario asociado a esta persona.
     *
     * @param user usuario asociado
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Verifica si la persona tiene foto de perfil.
     *
     * @return true si tiene foto
     */
    public boolean hasPhoto() {
        return photoData != null && photoData.length > 0;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios de la persona sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }
        if (fullName.length() < 1 || fullName.length() > 100) {
            throw new IllegalArgumentException("El nombre completo debe tener entre 1 y 100 caracteres");
        }
        if (personalEmail != null && personalEmail.length() > 254) {
            throw new IllegalArgumentException("El email personal debe tener máximo 254 caracteres");
        }
    }

    /**
     * Obtiene una representación resumida de la persona.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Persona: " + (fullName != null ? fullName : "Sin nombre") + 
               " (ID: " + personId + ")" +
               (personalEmail != null ? " - " + personalEmail : "");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return personId == 0;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string de la persona
     */
    @Override
    public String toString() {
        return "People{" +
                "personId=" + personId +
                ", fullName='" + fullName + '\'' +
                ", personalEmail='" + personalEmail + '\'' +
                ", hasPhoto=" + hasPhoto() +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}