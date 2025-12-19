package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.SubjectDTO;
import com.horarios.SGH.DTO.responseDTO;
import com.horarios.SGH.Service.SubjectService;
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

import java.util.List;

/**
 * Controlador REST para gestión de materias.
 * Proporciona operaciones CRUD para materias con validaciones específicas.
 * Implementa validación de nombres de materia y manejo de errores consistente.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Materias", description = "Endpoints para gestión de materias")
public class SubjectController extends AbstractController {

    private final SubjectService service;

    /**
     * Constantes para validación de nombres de materia.
     */
    private static final int MIN_SUBJECT_NAME_LENGTH = 5;
    private static final int MAX_SUBJECT_NAME_LENGTH = 20;

    /**
     * Valida el nombre de una materia según reglas de negocio.
     * Verifica que no contenga números y tenga longitud adecuada.
     *
     * @param subjectName Nombre de la materia a validar
     * @return Mensaje de error si no es válido, null si es válido
     */
    private String validateSubjectName(String subjectName) {
        if (subjectName == null) {
            return "El nombre de la materia es obligatorio";
        }
        if (subjectName.matches(".*\\d.*")) {
            return "El nombre de la materia no puede contener números";
        }
        if (subjectName.length() < MIN_SUBJECT_NAME_LENGTH) {
            return "El nombre de la materia debe tener al menos " + MIN_SUBJECT_NAME_LENGTH + " caracteres";
        }
        if (subjectName.length() > MAX_SUBJECT_NAME_LENGTH) {
            return "El nombre de la materia debe tener máximo " + MAX_SUBJECT_NAME_LENGTH + " caracteres";
        }
        return null; // Válido
    }

    /**
     * Crea una nueva materia en el sistema.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param dto Datos de la materia a crear
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PostMapping
    @Operation(summary = "Crear materia", description = "Crea una nueva materia con validación de nombre")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Materia creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o materia ya existente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class)))
    })
    public ResponseEntity<responseDTO> create(@Valid @RequestBody SubjectDTO dto, BindingResult bindingResult) {
        // Validar errores de validación del DTO
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("Error de validación");
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", errorMessage));
        }

        // Validación adicional del nombre de la materia
        String validationError = validateSubjectName(dto.getSubjectName());
        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", validationError));
        }

        try {
            service.create(dto);
            return successResponseDTO("Materia creada correctamente");
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("Materia ya existente");
        } catch (Exception e) {
            return errorResponseDTO("Error interno del servidor");
        }
    }

    // Obtener todas las materias
    @GetMapping
    public ResponseEntity<List<SubjectDTO>> getAll() {
        List<SubjectDTO> subjects = service.getAll();
        return ResponseEntity.ok(subjects);
    }

    // Obtener materia por ID
    @GetMapping("/{id}")
    public ResponseEntity<responseDTO> getById(@PathVariable int id) {
        if (id <= 0) {
            return errorResponseDTO("ID inválido o no encontrado");
        }
        try {
            service.getById(id); // Solo ejecuta, no guarda variable
            return successResponseDTO("Materia encontrada");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponseDTO("Materia no encontrada").getBody());
        }
    }

    /**
     * Actualiza los datos de una materia existente.
     * Valida los datos de entrada y verifica restricciones de integridad.
     *
     * @param id ID de la materia a actualizar
     * @param dto Nuevos datos de la materia
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con resultado de la operación
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar materia", description = "Actualiza los datos de una materia existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Materia actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación o nombre duplicado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = responseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Materia no encontrada")
    })
    public ResponseEntity<responseDTO> update(@PathVariable int id, @Valid @RequestBody SubjectDTO dto, BindingResult bindingResult) {
        // Validar errores de validación del DTO
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("Error de validación");
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", errorMessage));
        }

        // Validación adicional del nombre de la materia
        String validationError = validateSubjectName(dto.getSubjectName());
        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(new responseDTO("ERROR", validationError));
        }

        try {
            service.update(id, dto);
            return successResponseDTO("Materia actualizada correctamente");
        } catch (DataIntegrityViolationException e) {
            return errorResponseDTO("No pudes colocar el nombre de una materia ya existente");
        } catch (Exception e) {
            return errorResponseDTO("Error interno del servidor");
        }
    }

    // Eliminar materia
    @DeleteMapping("/{id}")
    public ResponseEntity<responseDTO> delete(@PathVariable int id) {
        try {
            service.delete(id);
            return successResponseDTO("Materia eliminada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorResponseDTO(e.getMessage()).getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponseDTO("Materia no encontrada").getBody());
        }
    }

}