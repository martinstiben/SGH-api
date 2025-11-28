package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.TeacherAvailabilityDTO;
import com.horarios.SGH.Model.Days;
import com.horarios.SGH.Model.TeacherAvailability;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Repository.ITeacherAvailabilityRepository;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
public class TeacherAvailabilityController {

    private final ITeacherAvailabilityRepository availabilityRepo;
    private final Iteachers teacherRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    @PostMapping("/register")
    @Operation(
        summary = "Registrar disponibilidad de profesor",
        description = "Registra la disponibilidad horaria de un profesor para un día específico"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Disponibilidad registrada correctamente",
        content = @Content(mediaType = "text/plain")
    )
    public String registerAvailability(@RequestBody @Schema(implementation = TeacherAvailabilityDTO.class) TeacherAvailabilityDTO dto) {
        // Validar que el profesor existe
        teachers teacher = teacherRepo.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado con ID: " + dto.getTeacherId()));

        // Validar que no exista ya disponibilidad para este profesor y día
        Days dayEnum = dto.getDay();
        List<TeacherAvailability> existing = availabilityRepo.findByTeacher_IdAndDay(dto.getTeacherId(), dayEnum);
        if (!existing.isEmpty()) {
            throw new RuntimeException("Ya existe disponibilidad registrada para este profesor en el día " + dto.getDay());
        }

        // Crear la nueva disponibilidad
        TeacherAvailability availability = new TeacherAvailability();
        availability.setTeacher(teacher);
        availability.setDay(dayEnum);
        availability.setAmStart(dto.getAmStart());
        availability.setAmEnd(dto.getAmEnd());
        availability.setPmStart(dto.getPmStart());
        availability.setPmEnd(dto.getPmEnd());

        // Validar que al menos tenga un horario válido
        if (!availability.hasValidSchedule()) {
            throw new RuntimeException("Debe proporcionar al menos un horario válido (mañana o tarde)");
        }

        availabilityRepo.save(availability);
        return "Disponibilidad registrada correctamente para " + teacher.getTeacherName() + " el día " + dto.getDay();
    }

    @PutMapping("/update")
    @Operation(
        summary = "Actualizar disponibilidad de profesor",
        description = "Actualiza la disponibilidad horaria de un profesor para un día específico"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Disponibilidad actualizada correctamente",
        content = @Content(mediaType = "text/plain")
    )
    public String updateAvailability(@RequestBody @Schema(implementation = TeacherAvailabilityDTO.class) TeacherAvailabilityDTO dto) {
        // Validar que el profesor existe
        teachers teacher = teacherRepo.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado con ID: " + dto.getTeacherId()));

        // Buscar disponibilidad existente
        Days dayEnum = dto.getDay();
        List<TeacherAvailability> existing = availabilityRepo.findByTeacher_IdAndDay(dto.getTeacherId(), dayEnum);
        if (existing.isEmpty()) {
            throw new RuntimeException("No existe disponibilidad registrada para este profesor en el día " + dto.getDay());
        }

        // Actualizar la disponibilidad existente
        TeacherAvailability availability = existing.get(0);
        availability.setAmStart(dto.getAmStart());
        availability.setAmEnd(dto.getAmEnd());
        availability.setPmStart(dto.getPmStart());
        availability.setPmEnd(dto.getPmEnd());

        // Validar que al menos tenga un horario válido
        if (!availability.hasValidSchedule()) {
            throw new RuntimeException("Debe proporcionar al menos un horario válido (mañana o tarde)");
        }

        availabilityRepo.save(availability);
        return "Disponibilidad actualizada correctamente para " + teacher.getTeacherName() + " el día " + dto.getDay();
    }

    @GetMapping("/by-teacher/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Consultar disponibilidad de un profesor",
        description = "Obtiene todos los horarios de disponibilidad de un profesor específico. Si no tiene disponibilidad, crea la configuración predeterminada."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de disponibilidades encontradas"),
        @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public List<TeacherAvailability> getAvailability(@PathVariable Integer id) {
        // Validar que el profesor existe
        teachers teacher = teacherRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado con ID: " + id));

        // Obtener disponibilidad actual
        List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_Id(id);

        // Si no tiene disponibilidad, crear la predeterminada
        if (availabilities.isEmpty()) {
            createDefaultAvailability(teacher);
            // Recargar después de crear
            availabilities = availabilityRepo.findByTeacher_Id(id);
        }

        return availabilities;
    }

    /**
     * Crea disponibilidad predeterminada para un profesor: 06:00am - 12:00pm Lunes a Viernes
     */
    private void createDefaultAvailability(teachers teacher) {
        // Días laborables: Lunes a Viernes
        Days[] workDays = {Days.Lunes, Days.Martes, Days.Miércoles, Days.Jueves, Days.Viernes};

        for (Days day : workDays) {
            TeacherAvailability availability = new TeacherAvailability();
            availability.setTeacher(teacher);
            availability.setDay(day);
            availability.setAmStart(LocalTime.parse("06:00"));
            availability.setAmEnd(LocalTime.parse("12:00"));
            // PM se deja null (no disponible)
            availability.setPmStart(null);
            availability.setPmEnd(null);

            availabilityRepo.save(availability);
        }
    }

    @DeleteMapping("/delete/{teacherId}/{day}")
    @Operation(
        summary = "Eliminar disponibilidad de profesor",
        description = "Elimina la disponibilidad horaria de un profesor para un día específico"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Disponibilidad eliminada correctamente",
        content = @Content(mediaType = "text/plain")
    )
    public String deleteAvailability(@PathVariable Integer teacherId, @PathVariable String day) {
        // Validar que el profesor existe
        teachers teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado con ID: " + teacherId));

        // Buscar disponibilidad existente
        Days dayEnum = Days.valueOf(day);
        List<TeacherAvailability> existing = availabilityRepo.findByTeacher_IdAndDay(teacherId, dayEnum);
        if (existing.isEmpty()) {
            throw new RuntimeException("No existe disponibilidad registrada para este profesor en el día " + day);
        }

        // Eliminar la disponibilidad
        availabilityRepo.delete(existing.get(0));
        return "Disponibilidad eliminada correctamente para " + teacher.getTeacherName() + " el día " + day;
    }

    @GetMapping("/available")
    public List<teachers> getAvailableTeachers(
            @RequestParam String day,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam Integer subjectId
    ) {
        LocalTime startTime = LocalTime.parse(start);
        LocalTime endTime = LocalTime.parse(end);

        // Filtrar docentes por materia usando TeacherSubject
        List<teachers> all = teacherSubjectRepo.findBySubject_Id(subjectId)
                .stream()
                .map(ts -> ts.getTeacher())
                .distinct()
                .collect(Collectors.toList());

        return all.stream().filter(t -> {
            List<TeacherAvailability> disponibilidad = availabilityRepo.findByTeacher_IdAndDay(t.getId(), Days.valueOf(day));
            return disponibilidad.stream().anyMatch(d -> {
                // Verificar si el horario solicitado está cubierto por AM o PM
                boolean coveredByAM = d.getAmStart() != null && d.getAmEnd() != null &&
                        !startTime.isBefore(d.getAmStart()) && !endTime.isAfter(d.getAmEnd());
                boolean coveredByPM = d.getPmStart() != null && d.getPmEnd() != null &&
                        !startTime.isBefore(d.getPmStart()) && !endTime.isAfter(d.getPmEnd());
                return coveredByAM || coveredByPM;
            });
        }).collect(Collectors.toList());
    }
}