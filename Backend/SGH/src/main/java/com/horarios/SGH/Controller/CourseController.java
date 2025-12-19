package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.DTO.CourseStudentDTO;
import com.horarios.SGH.DTO.responseDTO;
import com.horarios.SGH.Service.CourseService;
import com.horarios.SGH.Service.ValidationUtils;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

/**
 * Controlador REST para gestión de cursos.
 * Proporciona operaciones CRUD para cursos y consulta de estudiantes por curso.
 * Implementa validación de datos y manejo de errores consistente.
 * Extiende AbstractController para funcionalidades comunes.
 *
 * Patrones de diseño aplicados:
 * - Template Method: Para validación y manejo de errores
 * - Factory: Para creación de DTOs (delegado al servicio)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Cursos", description = "Endpoints para gestión de cursos")
public class CourseController extends AbstractController {

    private final CourseService service;

    /**
     * Crea un nuevo curso en el sistema.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param dto Datos del curso a crear
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PostMapping
    @Operation(summary = "Crear curso", description = "Crea un nuevo curso con validación de datos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Curso creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o curso ya existente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class)))
    })
    public ResponseEntity<responseDTO> create(@Valid @RequestBody CourseDTO dto, BindingResult bindingResult) {
        ResponseEntity<?> validationError = handleValidationErrors(bindingResult);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", (String) ((Map<String, String>) validationError.getBody()).get("error")));
        }

        try {
            ValidationUtils.validateCourseName(dto.getCourseName());
        } catch (IllegalArgumentException e) {
            return errorResponseDTO(e.getMessage());
        }

        try {
            service.create(dto);
            return successResponseDTO("Curso creado correctamente");
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("Curso ya existente");
        } catch (Exception e) {
            return errorResponseDTO("Error interno del servidor");
        }
    }

    /**
     * Obtiene todos los cursos disponibles.
     *
     * @return Lista de cursos
     */
    @GetMapping
    @Operation(summary = "Obtener todos los cursos", description = "Devuelve la lista completa de cursos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de cursos obtenida exitosamente")
    })
    public List<CourseDTO> getAll() {
        return service.getAll();
    }

    /**
     * Obtiene un curso específico por su ID.
     *
     * @param id ID del curso
     * @return Datos del curso encontrado
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener curso por ID", description = "Devuelve los datos de un curso específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Curso encontrado"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public CourseDTO getById(@PathVariable int id) {
        return service.getById(id);
    }

    /**
     * Obtiene la lista de estudiantes matriculados en un curso.
     * Requiere permisos de coordinador.
     *
     * @param id ID del curso
     * @return Lista de estudiantes del curso
     */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Obtener estudiantes de un curso", description = "Devuelve la lista de estudiantes matriculados en un curso específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de estudiantes obtenida exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID de curso inválido"),
        @ApiResponse(responseCode = "403", description = "No autorizado - requiere rol COORDINADOR")
    })
    public ResponseEntity<?> getStudentsByCourseId(@PathVariable int id) {
        try {
            List<CourseStudentDTO> students = service.getStudentsByCourseId(id);
            return ResponseEntity.ok(students);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new responseDTO("ERROR", "Error interno del servidor: " + e.getMessage()));
        }
    }

    /**
     * Actualiza los datos de un curso existente.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param id ID del curso a actualizar
     * @param dto Nuevos datos del curso
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar curso", description = "Actualiza los datos de un curso existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Curso actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o nombre duplicado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public ResponseEntity<responseDTO> update(@PathVariable int id, @Valid @RequestBody CourseDTO dto, BindingResult bindingResult) {
        ResponseEntity<?> validationError = handleValidationErrors(bindingResult);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new responseDTO("ERROR", (String) ((Map<String, String>) validationError.getBody()).get("error")));
        }

        try {
            ValidationUtils.validateCourseName(dto.getCourseName());
        } catch (IllegalArgumentException e) {
            return errorResponseDTO(e.getMessage());
        }

        try {
            service.update(id, dto);
            return successResponseDTO("Curso actualizado correctamente");
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("no puedes colocar el nombre de un curso ya existente");
        } catch (Exception e) {
            return errorResponseDTO("Error interno del servidor");
        }
    }

    /**
     * Elimina un curso del sistema.
     * Verifica que el curso no esté asociado a horarios antes de eliminarlo.
     *
     * @param id ID del curso a eliminar
     * @return ResponseEntity con resultado de la operación
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar curso", description = "Elimina un curso si no está asociado a horarios")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Curso eliminado exitosamente"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar - curso asociado a horario"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public ResponseEntity<responseDTO> delete(@PathVariable int id) {
        try {
            service.delete(id);
            return successResponseDTO("Curso eliminado correctamente");
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("No se puede eliminar el curso porque está asociado a un horario");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new responseDTO("ERROR", "Curso no encontrado"));
        }
    }
}