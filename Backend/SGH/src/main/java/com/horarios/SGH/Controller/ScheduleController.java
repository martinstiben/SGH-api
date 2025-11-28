package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.ScheduleGenerationDiagnosticDTO;
import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.DTO.CourseGenerationValidationDTO;
import com.horarios.SGH.DTO.ScheduleTableDTO;
import com.horarios.SGH.DTO.ScheduleVerificationReportDTO;
import com.horarios.SGH.Service.ScheduleGenerationService;
import com.horarios.SGH.Service.ScheduleHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "Horarios", description = "Gestión de generación automática de horarios")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleGenerationService generationService;
    private final ScheduleHistoryService historyService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Generar horarios automáticamente por cursos",
        description = "Genera horarios automáticamente para cursos que no tienen horario asignado. " +
                       "Utiliza únicamente el profesor asignado a cada curso y valida que cada profesor " +
                       "esté asociado a una sola materia."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horarios generados exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en parámetros o configuración de profesores"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ScheduleHistoryDTO generate(
            @Parameter(description = "Parámetros de generación de horarios", required = true)
            @RequestBody ScheduleHistoryDTO request,
            Authentication auth
    ) {
        return generationService.generate(request, auth.getName());
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Generar horarios automáticamente (interfaz simplificada)",
        description = "Genera horarios automáticamente para cursos sin asignación usando parámetros por defecto " +
                       "(semana actual, lunes a viernes). Ideal para botón en interfaz de usuario."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horarios generados exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en configuración de profesores"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ScheduleHistoryDTO autoGenerate(Authentication auth) {
        return generationService.autoGenerate(auth.getName());
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Regenerar todo el horario automáticamente",
        description = "Borra todos los horarios existentes y genera un horario completamente nuevo " +
                       "automáticamente para todos los cursos. Útil para reiniciar la planificación."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horario regenerado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en configuración de profesores"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ScheduleHistoryDTO regenerate(Authentication auth) {
        return generationService.regenerate(auth.getName());
    }

    @PostMapping("/generate-course/{courseId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Generar horario completo para un curso específico",
        description = "Genera automáticamente un horario completo para un curso seleccionado, asignando clases distribuidas inteligentemente a lo largo de la semana según disponibilidad del profesor. Ideal para crear horarios completos por curso desde la interfaz."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horario generado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Curso no válido o sin profesor asignado"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ScheduleHistoryDTO generateScheduleForCourse(
            @Parameter(description = "ID del curso para el cual generar horario", required = true, example = "1")
            @PathVariable Integer courseId,
            Authentication auth
    ) {
        return generationService.generateScheduleForCourse(courseId, auth.getName());
    }

    @PostMapping("/generate-course/{courseId}/quick")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Generar horario completo para un curso (interfaz simplificada)",
        description = "Versión simplificada del endpoint de generación por curso. No requiere parámetros adicionales, usa valores por defecto optimizados para una experiencia de un solo clic desde la interfaz de usuario."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horario generado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Curso no válido o sin profesor asignado"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ScheduleHistoryDTO generateScheduleForCourseQuick(
            @Parameter(description = "ID del curso para el cual generar horario", required = true, example = "1")
            @PathVariable Integer courseId,
            Authentication auth
    ) {
        logger.info("=== GENERACIÓN RÁPIDA DE HORARIO PARA CURSO {} ===", courseId);
        
        try {
            ScheduleHistoryDTO result = generationService.generateScheduleForCourse(courseId, auth.getName());
            
            logger.info("Generación rápida completada - Status: {}, Horarios generados: {}", 
                result.getStatus(), result.getTotalGenerated());
            
            return result;
        } catch (Exception e) {
            logger.error("Error en generación rápida para curso {}: {}", courseId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Obtener historial de generaciones",
        description = "Consulta el historial de todas las generaciones de horarios realizadas en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public Page<ScheduleHistoryDTO> history(
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return historyService.history(page, size);
    }

    @GetMapping("/diagnostic")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Diagnóstico completo del sistema de generación de horarios",
        description = "Obtiene un diagnóstico detallado del estado actual del sistema: cursos, profesores, disponibilidades, horarios existentes y posibles problemas. Útil para debugging."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Diagnóstico generado exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al generar el diagnóstico")
    })
    public ScheduleGenerationDiagnosticDTO generateDiagnostic() {
        return generationService.generateDiagnostic();
    }

    @GetMapping("/available-courses")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Obtener cursos disponibles para generación automática",
        description = "Lista todos los cursos que no tienen horario asignado pero sí tienen profesor asignado, " +
                     "listos para generación automática de horarios."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cursos disponibles obtenidos exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public List<CourseDTO> getCoursesAvailableForAutoGeneration() {
        return generationService.getCoursesAvailableForAutoGeneration();
    }

    @GetMapping("/validate-course/{courseId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Validar viabilidad de generación para un curso",
        description = "Valida si un curso específico puede tener horario generado automáticamente, " +
                     "incluyendo verificación de profesor, disponibilidad y posibles conflictos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validación completada"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public CourseGenerationValidationDTO validateCourseForGeneration(
            @Parameter(description = "ID del curso a validar", required = true, example = "1")
            @PathVariable Integer courseId) {
        return generationService.validateCourseForGeneration(courseId);
    }

    @GetMapping("/table/{courseId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Obtener horario de curso en formato tabla",
        description = "Obtiene el horario completo de un curso en formato de tabla visual, como se muestra en el ejemplo del curso 1A. Incluye días como columnas y franjas horarias como filas."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Horario obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ScheduleTableDTO getCourseScheduleTable(
            @Parameter(description = "ID del curso", required = true, example = "1")
            @PathVariable Integer courseId) {
        return generationService.getCourseScheduleTable(courseId);
    }

    @GetMapping("/verify/{courseId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(
        summary = "Verificar exhaustivamente el horario generado",
        description = "Realiza una verificación completa del horario generado para un curso específico, validando completitud, bloques protegidos, conflictos y formato."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación completada"),
        @ApiResponse(responseCode = "404", description = "Curso no encontrado"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ScheduleVerificationReportDTO verifyGeneratedSchedule(
            @Parameter(description = "ID del curso a verificar", required = true, example = "1")
            @PathVariable Integer courseId) {
        return generationService.verifyGeneratedSchedule(courseId);
    }

}