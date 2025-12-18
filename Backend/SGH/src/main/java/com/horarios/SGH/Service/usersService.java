package com.horarios.SGH.Service;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.horarios.SGH.Model.User;
import com.horarios.SGH.Repository.IUserRepository;

/**
 * Servicio para gestión de usuarios con optimizaciones de rendimiento
 * y manejo robusto de excepciones.
 * 
 * @author Sistema SGH
 * @version 1.0
 */
@Service
@Transactional
public class usersService {

    private static final Logger logger = Logger.getLogger(usersService.class.getName());

    private final IUserRepository usersRepository;
    private final FileStorageService fileStorageService;

    /**
     * Constructor con inyección de dependencias usando el patrón recomendado.
     * 
     * @param usersRepository repositorio de usuarios
     * @param fileStorageService servicio de almacenamiento de archivos
     */
    public usersService(IUserRepository usersRepository, FileStorageService fileStorageService) {
        this.usersRepository = usersRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Encuentra un usuario por su ID.
     * 
     * @param userId ID del usuario
     * @return Optional con el usuario encontrado o vacío si no existe
     * @throws IllegalArgumentException si userId es null o negativo
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser un número positivo");
        }
        
        try {
            logger.log(Level.INFO, "Buscando usuario con ID: {0}", userId);
            return usersRepository.findById(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener el usuario con ID: " + userId, e);
            throw new RuntimeException("Error al obtener el usuario con ID: " + userId, e);
        }
    }

    /**
     * Encuentra un usuario por email de forma optimizada.
     * CORRECCIÓN CRÍTICA: Elimina la consulta N+1 que cargaba todos los usuarios.
     * 
     * @param email Email del usuario a buscar
     * @return Usuario encontrado o null si no existe
     * @throws IllegalArgumentException si email es null o vacío
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        
        try {
            logger.log(Level.INFO, "Buscando usuario por email: {0}", normalizedEmail);
            
            // MÉTODO OPTIMIZADO: Usa la consulta nativa del repositorio en lugar de cargar todos los usuarios
            Optional<User> userOpt = usersRepository.findByPerson_Email(normalizedEmail);
            
            if (userOpt.isPresent()) {
                logger.log(Level.INFO, "Usuario encontrado por email: {0}", normalizedEmail);
                return userOpt.get();
            }
            
            logger.log(Level.INFO, "No se encontró usuario con email: {0}", normalizedEmail);
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al buscar usuario por email: " + normalizedEmail, e);
            throw new RuntimeException("Error al buscar usuario por email: " + normalizedEmail, e);
        }
    }

    /**
     * Actualiza la foto de perfil de un usuario con validación robusta.
     * 
     * @param userId ID del usuario
     * @param photo Archivo de imagen para la foto de perfil
     * @return Mensaje de confirmación
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    public String updateUserPhoto(Long userId, MultipartFile photo) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser un número positivo");
        }

        try {
            Optional<User> userOpt = usersRepository.findById(userId);
            if (!userOpt.isPresent()) {
                throw new IllegalArgumentException("Usuario no encontrado con ID: " + userId);
            }

            User user = userOpt.get();

            if (photo != null && !photo.isEmpty()) {
                // Validar tipo de archivo
                if (!isValidImageType(photo.getContentType())) {
                    throw new IllegalArgumentException("Tipo de archivo no válido. Solo se permiten imágenes JPEG, PNG y GIF");
                }
                
                // Validar tamaño (máximo 5MB)
                if (photo.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("El archivo es demasiado grande. Máximo 5MB permitido");
                }

                FileStorageService.PhotoData photoData = fileStorageService.processImageFile(photo);
                user.getPerson().setPhotoData(photoData.getData());
                user.getPerson().setPhotoContentType(photoData.getContentType());
                user.getPerson().setPhotoFileName(photoData.getFileName());
                
                logger.log(Level.INFO, "Foto actualizada para usuario ID: {0}", userId);
            } else {
                // Si photo es null o vacío, eliminar foto existente
                user.getPerson().setPhotoData(null);
                user.getPerson().setPhotoContentType(null);
                user.getPerson().setPhotoFileName(null);
                
                logger.log(Level.INFO, "Foto eliminada para usuario ID: {0}", userId);
            }

            usersRepository.save(user);
            return "Foto de perfil actualizada correctamente";

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Error de validación actualizando foto para usuario {0}: {1}", 
                      new Object[]{userId, e.getMessage()});
            throw e; // Re-lanzar excepciones de validación
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al actualizar la foto de perfil para usuario " + userId, e);
            throw new RuntimeException("Error al actualizar la foto de perfil: " + e.getMessage(), e);
        }
    }

    /**
     * Encuentra todos los usuarios por rol específico usando consulta optimizada.
     * 
     * @param roleName Nombre del rol a buscar
     * @return Lista de usuarios con el rol especificado
     * @throws IllegalArgumentException si roleName es null o vacío
     */
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rol no puede estar vacío");
        }

        try {
            logger.log(Level.INFO, "Buscando usuarios por rol: {0}", roleName);
            
            // Usar consulta optimizada que carga las relaciones con JOIN FETCH
            List<User> users = usersRepository.findByRoleNameWithDetails(roleName.trim());
            
            logger.log(Level.INFO, "Encontrados {0} usuarios con rol: {1}", 
                      new Object[]{users.size(), roleName});
            
            return users;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener usuarios por rol: " + roleName, e);
            throw new RuntimeException("Error al obtener usuarios por rol: " + roleName, e);
        }
    }

    /**
     * Obtiene la información completa de un usuario incluyendo foto.
     * 
     * @param userId ID del usuario
     * @return DTO con información del usuario
     * @throws IllegalArgumentException si userId es inválido
     */
    @Transactional(readOnly = true)
    public Optional<com.horarios.SGH.DTO.usersDTO> getUserWithPhoto(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser un número positivo");
        }

        try {
            logger.log(Level.INFO, "Obteniendo información completa del usuario ID: {0}", userId);
            
            return usersRepository.findById(userId)
                .map(user -> {
                    com.horarios.SGH.DTO.usersDTO dto = new com.horarios.SGH.DTO.usersDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUserName(user.getPerson().getFullName());
                    dto.setPhotoData(user.getPerson().getPhotoData());
                    dto.setPhotoContentType(user.getPerson().getPhotoContentType());
                    dto.setPhotoFileName(user.getPerson().getPhotoFileName());
                    return dto;
                });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener el usuario ID: " + userId, e);
            throw new RuntimeException("Error al obtener el usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un tipo de imagen es válido.
     * 
     * @param contentType tipo de contenido a verificar
     * @return true si el tipo es válido
     */
    private boolean isValidImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return contentType.equals("image/jpeg") || 
               contentType.equals("image/jpg") || 
               contentType.equals("image/png") || 
               contentType.equals("image/gif");
    }

    /**
     * Obtiene el número total de usuarios en el sistema.
     * 
     * @return número total de usuarios
     */
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        try {
            long count = usersRepository.count();
            logger.log(Level.INFO, "Total de usuarios en el sistema: {0}", count);
            return count;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener el conteo total de usuarios", e);
            throw new RuntimeException("Error al obtener el conteo de usuarios", e);
        }
    }

    /**
     * Verifica si existe un usuario con el email especificado.
     * 
     * @param email email a verificar
     * @return true si existe un usuario con ese email
     * @throws IllegalArgumentException si email es null o vacío
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        
        try {
            String normalizedEmail = email.trim().toLowerCase();
            return usersRepository.existsByPerson_Email(normalizedEmail);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error verificando existencia de email: " + email, e);
            throw new RuntimeException("Error verificando existencia de email", e);
        }
    }
}