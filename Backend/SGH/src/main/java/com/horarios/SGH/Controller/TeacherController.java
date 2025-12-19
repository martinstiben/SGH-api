package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.TeacherDTO;
import com.horarios.SGH.DTO.responseDTO;
import com.horarios.SGH.Service.SubjectService;
import com.horarios.SGH.Service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Controlador REST para gestión de docentes.
 * Proporciona operaciones CRUD para docentes, gestión de fotos de perfil
 * y validaciones específicas de nombres de docentes.
 * Implementa validación de datos y manejo de errores consistente.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Docentes", description = "Endpoints para gestión de docentes")
public class TeacherController extends AbstractController {

    private final TeacherService teacherService;
    private final SubjectService subjectService;

    /**
     * Constantes para validación de nombres de docente.
     */
    private static final int MIN_TEACHER_NAME_LENGTH = 5;
    private static final int MAX_TEACHER_NAME_LENGTH = 50;

    /**
     * Valida el nombre de un docente según reglas de negocio.
     * Verifica que no contenga números y tenga longitud adecuada.
     *
     * @param teacherName Nombre del docente a validar
     * @return Mensaje de error si no es válido, null si es válido
     */
    private String validateTeacherName(String teacherName) {
        if (teacherName == null) {
            return "El nombre del docente es obligatorio";
        }
        if (teacherName.matches(".*\\d.*")) {
            return "El nombre del docente no puede contener números";
        }
        if (teacherName.length() < MIN_TEACHER_NAME_LENGTH) {
            return "El nombre del docente debe tener al menos " + MIN_TEACHER_NAME_LENGTH + " caracteres";
        }
        if (teacherName.length() > MAX_TEACHER_NAME_LENGTH) {
            return "El nombre del docente debe tener máximo " + MAX_TEACHER_NAME_LENGTH + " caracteres";
        }
        return null; // Válido
    }

    /**
     * Crea un nuevo docente en el sistema.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param dto Datos del docente a crear
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PostMapping
    @Operation(summary = "Crear docente", description = "Crea un nuevo docente con validación de nombre y materia")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Docente creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o materia inexistente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class)))
    })
    public ResponseEntity<responseDTO> create(@Valid @RequestBody TeacherDTO dto, BindingResult bindingResult) {
        try {
            // Validar errores de validación del DTO
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .findFirst()
                        .orElse("Error de validación");
                return errorResponseDTO(errorMessage);
            }

            // Validación adicional del nombre del docente
            String validationError = validateTeacherName(dto.getTeacherName());
            if (validationError != null) {
                return errorResponseDTO(validationError);
            }

            // Verificar que la materia existe si subjectId > 0
            if (dto.getSubjectId() > 0) {
                if (subjectService.getById(dto.getSubjectId()) == null) {
                    return errorResponseDTO("La materia con ID " + dto.getSubjectId() + " no existe");
                }
            }

            teacherService.create(dto);
            return successResponseDTO("Docente creado correctamente");
        } catch (Exception e) {
            return errorResponseDTO(e.getMessage());
        }
    }

    /**
     * Obtiene todos los docentes disponibles.
     *
     * @return Lista de docentes
     */
    @GetMapping
    @Operation(summary = "Obtener todos los docentes", description = "Devuelve la lista completa de docentes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de docentes obtenida exitosamente")
    })
    public ResponseEntity<List<TeacherDTO>> getAll() {
        return ResponseEntity.ok(teacherService.getAll());
    }

    /**
     * Obtiene un docente específico por su ID.
     *
     * @param id ID del docente
     * @return Datos del docente encontrado
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener docente por ID", description = "Devuelve los datos de un docente específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Docente encontrado"),
        @ApiResponse(responseCode = "400", description = "ID inválido"),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<TeacherDTO> getById(@PathVariable int id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        try {
            TeacherDTO teacher = teacherService.getById(id);
            if (teacher == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Actualiza los datos de un docente existente.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param id ID del docente a actualizar
     * @param dto Nuevos datos del docente
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar docente", description = "Actualiza los datos de un docente existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Docente actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o materia inexistente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<responseDTO> update(@PathVariable int id, @Valid @RequestBody TeacherDTO dto, BindingResult bindingResult) {
        try {
            // Validar errores de validación del DTO
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .findFirst()
                        .orElse("Error de validación");
                return errorResponseDTO(errorMessage);
            }

            // Verificar que la materia existe si subjectId > 0
            if (dto.getSubjectId() > 0) {
                if (subjectService.getById(dto.getSubjectId()) == null) {
                    return errorResponseDTO("La materia con ID " + dto.getSubjectId() + " no existe");
                }
            }

            teacherService.update(id, dto);
            return successResponseDTO("Docente actualizado correctamente");
        } catch (Exception e) {
            return errorResponseDTO(e.getMessage());
        }
    }

    /**
     * Elimina un docente del sistema.
     * Verifica que no tenga horarios asignados antes de eliminar.
     *
     * @param id ID del docente a eliminar
     * @return ResponseEntity con resultado de la operación
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar docente", description = "Elimina un docente verificando que no tenga horarios asignados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Docente eliminado exitosamente"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar porque tiene dependencias",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<responseDTO> delete(@PathVariable int id) {
        try {
            teacherService.delete(id);
            return successResponseDTO("Docente eliminado correctamente");
        } catch (IllegalStateException e) {
            return errorResponseDTO(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("No se puede eliminar el docente porque tiene dependencias");
        } catch (Exception e) {
            return errorResponseDTO(e.getMessage());
        }
    }

    /**
     * Actualiza la foto de perfil de un docente.
     *
     * @param id ID del docente
     * @param photo Archivo de imagen para la foto de perfil
     * @return ResponseEntity con resultado de la operación
     */
    @PutMapping("/{id}/photo")
    @Operation(summary = "Actualizar foto de perfil", description = "Actualiza la foto de perfil de un docente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Foto actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación en el archivo"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<responseDTO> updateTeacherPhoto(@PathVariable int id, @RequestParam("photo") MultipartFile photo) {
        try {
            String result = teacherService.updateTeacherPhoto(id, photo);
            return successResponseDTO(result);
        } catch (IllegalArgumentException e) {
            return errorResponseDTO(e.getMessage());
        } catch (Exception e) {
            return errorResponseDTO("Error al actualizar foto: " + e.getMessage());
        }
    }

    /**
     * Elimina la foto de perfil de un docente.
     *
     * @param id ID del docente
     * @return ResponseEntity con resultado de la operación
     */
    @DeleteMapping("/{id}/photo")
    @Operation(summary = "Eliminar foto de perfil", description = "Elimina la foto de perfil de un docente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Foto eliminada exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<responseDTO> deleteTeacherPhoto(@PathVariable int id) {
        try {
            String result = teacherService.updateTeacherPhoto(id, null);
            return successResponseDTO(result);
        } catch (Exception e) {
            return errorResponseDTO("Error al eliminar foto: " + e.getMessage());
        }
    }

    /**
     * Obtiene la foto de perfil de un docente.
     *
     * @param id ID del docente
     * @return ResponseEntity con los datos de la imagen
     */
    @GetMapping("/{id}/photo")
    @Operation(summary = "Obtener foto de perfil", description = "Obtiene la foto de perfil de un docente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Foto obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Foto no encontrada")
    })
    public ResponseEntity<byte[]> getTeacherPhoto(@PathVariable int id) {
        try {
            TeacherDTO teacher = teacherService.getById(id);
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