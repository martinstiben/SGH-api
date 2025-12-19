package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
* DTO para transferencia de datos de usuarios del sistema SGH.
* Extiende AbstractDTO implementando el patrón Abstract Factory
* con validaciones específicas para usuarios.
*
* Proporciona una representación simplificada de la entidad User
* optimizada para transferencia y presentación en APIs REST.
*
* Principios SOLID aplicados:
* - SRP: Responsabilidad única de representar usuarios
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
public class UserDTO extends AbstractDTO {

   /**
    * Identificador único del usuario en el sistema.
    * Generado automáticamente por la base de datos.
    */
   private Long userId;

   /**
    * Nombre de usuario único para autenticación.
    * Debe ser único en todo el sistema.
    */
   @NotNull(message = "El nombre de usuario es obligatorio")
   @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
   private String username;

   /**
    * Dirección de correo electrónico del usuario.
    * Debe ser única y válida según estándares RFC 5322.
    */
   @NotNull(message = "El email es obligatorio")
   @Email(message = "El email debe ser válido")
   @Size(max = 254, message = "El email debe tener máximo 254 caracteres")
   private String email;

   /**
    * Nombre real del usuario.
    * Parte del nombre completo junto con apellido.
    */
   @NotNull(message = "El nombre es obligatorio")
   @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
   private String firstName;

   /**
    * Apellido del usuario.
    * Parte del nombre completo junto con nombre.
    */
   @NotNull(message = "El apellido es obligatorio")
   @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
   private String lastName;

   /**
    * Fecha y hora de creación del usuario.
    * Establecida automáticamente al crear el registro.
    */
   private LocalDateTime createdAt;

   /**
    * Fecha y hora de última actualización del usuario.
    * Actualizada automáticamente en cada modificación.
    */
   private LocalDateTime updatedAt;

   /**
    * Constructor por defecto.
    */
   public UserDTO() {
       super();
   }

   /**
    * Constructor con parámetros principales.
    *
    * @param userId ID del usuario
    * @param username Nombre de usuario
    * @param email Correo electrónico
    * @param firstName Nombre
    * @param lastName Apellido
    */
   public UserDTO(Long userId, String username, String email, String firstName, String lastName) {
       super();
       this.userId = userId;
       this.username = username;
       this.email = email;
       this.firstName = firstName;
       this.lastName = lastName;
       this.createdAt = LocalDateTime.now();
   }

   // Getters y Setters
   public Long getUserId() {
       return userId;
   }

   public void setUserId(Long userId) {
       this.userId = userId;
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

   public String getFirstName() {
       return firstName;
   }

   public void setFirstName(String firstName) {
       this.firstName = firstName;
   }

   public String getLastName() {
       return lastName;
   }

   public void setLastName(String lastName) {
       this.lastName = lastName;
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
    * Método Factory para crear un UserDTO básico.
    * Implementa patrón Factory Method para instancias comunes.
    *
    * @param userId ID del usuario
    * @param username Nombre de usuario
    * @param email Correo electrónico
    * @param firstName Nombre
    * @param lastName Apellido
    * @return UserDTO con los datos proporcionados
    */
   public static UserDTO create(Long userId, String username, String email, String firstName, String lastName) {
       UserDTO dto = new UserDTO();
       dto.setUserId(userId);
       dto.setUsername(username);
       dto.setEmail(email);
       dto.setFirstName(firstName);
       dto.setLastName(lastName);
       dto.setCreatedAt(LocalDateTime.now());
       return dto;
   }

   /**
    * Método Factory para crear un UserDTO vacío.
    * Implementa patrón Factory Method para instancias comunes.
    *
    * @return UserDTO con valores por defecto
    */
   public static UserDTO empty() {
       UserDTO dto = new UserDTO();
       dto.setCreatedAt(LocalDateTime.now());
       return dto;
   }

   /**
    * Obtiene el nombre completo del usuario.
    * Concatena nombre y apellido con espacio.
    *
    * @return Nombre completo formateado
    */
   public String getFullName() {
       if (firstName == null && lastName == null) {
           return null;
       }
       if (firstName == null) {
           return lastName;
       }
       if (lastName == null) {
           return firstName;
       }
       return firstName + " " + lastName;
   }

   /**
    * Valida si el DTO tiene todos los campos obligatorios.
    * Método de validación de negocio.
    *
    * @return true si todos los campos obligatorios están presentes
    */
   @Override
   public boolean isValid() {
       return username != null && !username.trim().isEmpty() &&
              email != null && !email.trim().isEmpty() &&
              firstName != null && !firstName.trim().isEmpty() &&
              lastName != null && !lastName.trim().isEmpty();
   }

   /**
    * Obtiene una representación resumida del usuario.
    * Formato: "[username] ([email]) - [firstName] [lastName]"
    *
    * @return Representación resumida
    */
   @Override
   public String getSummary() {
       return String.format("%s (%s) - %s %s",
               username != null ? username : "Sin usuario",
               email != null ? email : "Sin email",
               firstName != null ? firstName : "Sin nombre",
               lastName != null ? lastName : "Sin apellido");
   }
}