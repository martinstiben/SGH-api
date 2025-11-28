package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.ScheduleGenerationDiagnosticDTO;
import com.horarios.SGH.Service.ScheduleGenerationService;
import com.horarios.SGH.Service.ScheduleHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "Horarios", description = "Gestión de generación automática de horarios")
public class ScheduleController {

    private final ScheduleGenerationService generationService;
    private final ScheduleHistoryService historyService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
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
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
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
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
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

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
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
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
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

    @GetMapping("/debug-courses")
    @Operation(
        summary = "Debug: Ver estado de cursos",
        description = "Endpoint temporal para verificar cursos y horarios"
    )
    public String debugCourses() {
        return generationService.debugCoursesStatus();
    }

    @DeleteMapping("/clear-all")
    @PreAuthorize("hasAuthority('ROLE_COORDINADOR')")
    @Operation(
        summary = "Limpiar todos los horarios",
        description = "Elimina todos los horarios existentes para testing"
    )
    public String clearAllSchedules() {
        generationService.clearAllSchedulesForTesting();
        return "Todos los horarios han sido eliminados";
    }


}