package com.horarios.SGH.Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import com.horarios.SGH.DTO.responseDTO;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Service.usersService;
import com.horarios.SGH.Repository.Iusers;

@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500", "http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/users")
public class usersController {

    @Autowired
    private usersService usersService;

    @Autowired
    private Iusers usersRepository;

    @Value("${app.master.username}")
    private String masterUsername;

    // Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id) {
        try {
            Optional<users> usuarioOptional = usersService.findById(id);
            if (usuarioOptional.isPresent()) {
                return ResponseEntity.ok(usuarioOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new responseDTO("ERROR", "Usuario no encontrado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error interno: " + e.getMessage()));
        }
    }

    // Login endpoint removido - ahora se maneja en AuthController con 2FA

    // Eliminar usuario (excepto master)
    @DeleteMapping("/username/{username}")
    public ResponseEntity<responseDTO> deleteUser(@PathVariable String username) {
        try {
            if (username.equalsIgnoreCase(masterUsername)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new responseDTO("ERROR", "No se puede eliminar el usuario master"));
            }

            Optional<users> usuario = usersRepository.findByUserName(username);
            if (!usuario.isPresent() || !usuario.get().getPerson().getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new responseDTO("ERROR", "Usuario no encontrado"));
            }

            usersRepository.delete(usuario.get());
            return ResponseEntity.ok(new responseDTO("OK", "Usuario eliminado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error interno: " + e.getMessage()));
        }
    }

    // Actualizar foto de perfil
    @PutMapping("/{id}/photo")
    public ResponseEntity<responseDTO> updateUserPhoto(@PathVariable int id, @RequestParam("photo") MultipartFile photo) {
        try {
            String result = usersService.updateUserPhoto(id, photo);
            return ResponseEntity.ok(new responseDTO("OK", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error al actualizar foto: " + e.getMessage()));
        }
    }

    // Eliminar foto de perfil
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<responseDTO> deleteUserPhoto(@PathVariable int id) {
        try {
            String result = usersService.updateUserPhoto(id, null);
            return ResponseEntity.ok(new responseDTO("OK", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error al eliminar foto: " + e.getMessage()));
        }
    }

    // Obtener foto de perfil
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getUserPhoto(@PathVariable int id) {
        try {
            Optional<com.horarios.SGH.DTO.usersDTO> userOpt = usersService.getUserWithPhoto(id);
            if (!userOpt.isPresent() || userOpt.get().getPhotoData() == null) {
                return ResponseEntity.notFound().build();
            }

            com.horarios.SGH.DTO.usersDTO user = userOpt.get();
            return ResponseEntity.ok()
                    .header("Content-Type", user.getPhotoContentType() != null ? user.getPhotoContentType() : "image/jpeg")
                    .header("Content-Disposition", "inline; filename=\"" + (user.getPhotoFileName() != null ? user.getPhotoFileName() : "photo.jpg") + "\"")
                    .body(user.getPhotoData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Obtener todos los usuarios
    @GetMapping
    public ResponseEntity<java.util.List<com.horarios.SGH.DTO.usersDTO>> getAllUsers() {
        try {
            java.util.List<users> allUsers = usersRepository.findAll();
            java.util.List<com.horarios.SGH.DTO.usersDTO> userDTOs = new java.util.ArrayList<>();

            for (users user : allUsers) {
                com.horarios.SGH.DTO.usersDTO dto = new com.horarios.SGH.DTO.usersDTO();
                dto.setUserId(user.getUserId());
                dto.setUserName(user.getPerson().getFullName());
                dto.setPassword(user.getPasswordHash());
                dto.setRole(com.horarios.SGH.Model.Role.valueOf(user.getRole().getRoleName()));
                dto.setPhotoData(user.getPerson().getPhotoData());
                dto.setPhotoContentType(user.getPerson().getPhotoContentType());
                dto.setPhotoFileName(user.getPerson().getPhotoFileName());
                userDTOs.add(dto);
            }

            return ResponseEntity.ok(userDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Eliminar usuario por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<responseDTO> deleteUserById(@PathVariable int id) {
        try {
            if (masterUsername != null && masterUsername.equals(String.valueOf(id))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new responseDTO("ERROR", "No se puede eliminar el usuario master"));
            }

            Optional<users> usuario = usersRepository.findById(id);
            if (!usuario.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new responseDTO("ERROR", "Usuario no encontrado"));
            }

            usersRepository.deleteById(id);
            return ResponseEntity.ok(new responseDTO("OK", "Usuario eliminado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error interno: " + e.getMessage()));
        }
    }
}