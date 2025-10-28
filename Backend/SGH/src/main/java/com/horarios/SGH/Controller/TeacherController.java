package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.TeacherDTO;
import com.horarios.SGH.DTO.responseDTO;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeacherController {

    private final TeacherService service;
    private final Isubjects Isubjects;

    @PostMapping
    public ResponseEntity<responseDTO> create(@Valid @RequestBody TeacherDTO dto, BindingResult bindingResult) {
        try {
            // Validar errores de validación del DTO
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .findFirst()
                        .orElse("Error de validación");
                return ResponseEntity.badRequest()
                        .body(new responseDTO("ERROR", errorMessage));
            }

            // VALIDACIÓN MANUAL ADICIONAL: Verificar que el nombre NO contenga números
            if (dto.getTeacherName() != null && dto.getTeacherName().matches(".*\\d.*")) {
                return ResponseEntity.badRequest()
                        .body(new responseDTO("ERROR", "El nombre del profesor no puede contener números"));
            }

            // VALIDACIÓN MANUAL ADICIONAL: Verificar que el nombre NO contenga números
            if (dto.getTeacherName() != null && dto.getTeacherName().matches(".*\\d.*")) {
                return ResponseEntity.badRequest()
                        .body(new responseDTO("ERROR", "El nombre del profesor no puede contener números"));
            }

            // VALIDACIÓN MANUAL ADICIONAL: Verificar longitud
            if (dto.getTeacherName() != null) {
                if (dto.getTeacherName().length() < 5) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "El nombre del profesor debe tener al menos 5 caracteres"));
                }
                if (dto.getTeacherName().length() > 50) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "El nombre del profesor debe tener máximo 50 caracteres"));
                }
            }

            // VALIDACIÓN MANUAL ADICIONAL: Verificar que el nombre NO contenga números
            if (dto.getTeacherName() != null && dto.getTeacherName().matches(".*\\d.*")) {
                return ResponseEntity.badRequest()
                        .body(new responseDTO("ERROR", "El nombre del profesor no puede contener números"));
            }

            // VALIDACIÓN MANUAL ADICIONAL: Verificar longitud
            if (dto.getTeacherName() != null) {
                if (dto.getTeacherName().length() < 5) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "El nombre del profesor debe tener al menos 5 caracteres"));
                }
                if (dto.getTeacherName().length() > 50) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "El nombre del profesor debe tener máximo 50 caracteres"));
                }
            }

            // Verificar que la materia existe si subjectId > 0
            if (dto.getSubjectId() > 0) {
                Optional<subjects> subject = Isubjects.findById(dto.getSubjectId());
                if (subject.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "La materia con ID " + dto.getSubjectId() + " no existe"));
                }
            }
            
            service.create(dto);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new responseDTO("OK", "Docente creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherDTO> getById(@PathVariable int id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        try {
            TeacherDTO teacher = service.getById(id);
            if (teacher == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<responseDTO> update(@PathVariable int id, @Valid @RequestBody TeacherDTO dto, BindingResult bindingResult) {
        try {
            // Validar errores de validación del DTO
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .findFirst()
                        .orElse("Error de validación");
                return ResponseEntity.badRequest()
                        .body(new responseDTO("ERROR", errorMessage));
            }

            // Verificar que la materia existe si subjectId > 0
            if (dto.getSubjectId() > 0) {
                Optional<subjects> subject = Isubjects.findById(dto.getSubjectId());
                if (subject.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new responseDTO("ERROR", "La materia con ID " + dto.getSubjectId() + " no existe"));
                }
            }
            
            service.update(id, dto);
            return ResponseEntity.ok(new responseDTO("OK", "Docente actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<responseDTO> delete(@PathVariable int id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(new responseDTO("OK", "Docente eliminado correctamente"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", "No se puede eliminar el docente porque tiene dependencias"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", e.getMessage()));
        }
    }

    // Actualizar foto de perfil del profesor
    @PutMapping("/{id}/photo")
    public ResponseEntity<responseDTO> updateTeacherPhoto(@PathVariable int id, @RequestParam("photo") MultipartFile photo) {
        try {
            String result = service.updateTeacherPhoto(id, photo);
            return ResponseEntity.ok(new responseDTO("OK", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error al actualizar foto: " + e.getMessage()));
        }
    }

    // Eliminar foto de perfil del profesor
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<responseDTO> deleteTeacherPhoto(@PathVariable int id) {
        try {
            String result = service.updateTeacherPhoto(id, null);
            return ResponseEntity.ok(new responseDTO("OK", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error al eliminar foto: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getTeacherPhoto(@PathVariable int id) {
        try {
            TeacherDTO teacher = service.getById(id);
            if (teacher == null || teacher.getPhotoData() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Type", teacher.getPhotoContentType() != null ? teacher.getPhotoContentType() : "image/jpeg")
                    .header("Content-Disposition", "inline; filename=\"" + (teacher.getPhotoFileName() != null ? teacher.getPhotoFileName() : "photo.jpg") + "\"")
                    .body(teacher.getPhotoData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}