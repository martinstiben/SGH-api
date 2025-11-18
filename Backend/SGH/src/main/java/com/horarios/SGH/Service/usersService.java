package com.horarios.SGH.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.horarios.SGH.Model.users;
import com.horarios.SGH.Model.Role;
import com.horarios.SGH.Repository.Iusers;

@Service
public class usersService {

    @Autowired
    private Iusers usersRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Optional<users> findById(int userId) {
        try {
            return usersRepository.findById(userId);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el usuario con ID: " + userId + ", Error: " + e.getMessage());
        }
    }

    // Método de login removido - ahora se maneja en AuthService con 2FA

    /**
     * Actualiza la foto de perfil de un usuario.
     * @param userId ID del usuario
     * @param photo Archivo de imagen para la foto de perfil
     * @return Mensaje de confirmación
     */
    public String updateUserPhoto(int userId, MultipartFile photo) {
        try {
            Optional<users> userOpt = usersRepository.findById(userId);
            if (!userOpt.isPresent()) {
                throw new IllegalArgumentException("Usuario no encontrado");
            }

            users user = userOpt.get();

            if (photo != null && !photo.isEmpty()) {
                FileStorageService.PhotoData photoData = fileStorageService.processImageFile(photo);
                user.getPerson().setPhotoData(photoData.getData());
                user.getPerson().setPhotoContentType(photoData.getContentType());
                user.getPerson().setPhotoFileName(photoData.getFileName());
            } else {
                // Si photo es null o vacío, eliminar foto existente
                user.getPerson().setPhotoData(null);
                user.getPerson().setPhotoContentType(null);
                user.getPerson().setPhotoFileName(null);
            }

            usersRepository.save(user);
            return "Foto de perfil actualizada correctamente";

        } catch (IllegalArgumentException e) {
            throw e; // Re-lanzar excepciones de validación
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar la foto de perfil: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encuentra todos los usuarios por rol específico
     * @param roleName Nombre del rol a buscar
     * @return Lista de usuarios con el rol especificado
     */
    public java.util.List<users> findUsersByRole(String roleName) {
        try {
            // Usar consulta optimizada que carga las relaciones
            return usersRepository.findByRoleNameWithDetails(roleName);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener usuarios por rol: " + roleName + ", Error: " + e.getMessage());
        }
    }

    /**
     * Obtiene la información completa de un usuario incluyendo foto.
     * @param userId ID del usuario
     * @return DTO con información del usuario
     */
    public Optional<com.horarios.SGH.DTO.usersDTO> getUserWithPhoto(int userId) {
        try {
            return usersRepository.findById(userId)
                .map(user -> {
                    com.horarios.SGH.DTO.usersDTO dto = new com.horarios.SGH.DTO.usersDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUserName(user.getPerson().getFullName());
                    dto.setPassword(user.getPasswordHash());
                    dto.setRole(Role.valueOf(user.getRole().getRoleName()));
                    dto.setPhotoData(user.getPerson().getPhotoData());
                    dto.setPhotoContentType(user.getPerson().getPhotoContentType());
                    dto.setPhotoFileName(user.getPerson().getPhotoFileName());
                    return dto;
                });
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el usuario: " + e.getMessage(), e);
        }
    }
}