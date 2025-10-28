package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Repository.IScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio refactorizado para exportación de horarios aplicando principios SOLID.
 * Utiliza el patrón Strategy para diferentes formatos de exportación.
 *
 * Principios SOLID aplicados:
 * - SRP: Una responsabilidad por clase
 * - OCP: Abierto para extensión, cerrado para modificación
 * - LSP: Las estrategias son intercambiables
 * - ISP: Interfaces específicas y pequeñas
 * - DIP: Depende de abstracciones, no de implementaciones concretas
 */
@Service
@RequiredArgsConstructor
public class ScheduleExportServiceRefactored {

    private final IScheduleRepository scheduleRepository;

    /**
     * Exporta horarios por curso en el formato especificado.
     *
     * @param courseId ID del curso
     * @param strategy Estrategia de exportación a utilizar
     * @return Array de bytes con el contenido exportado
     */
    public byte[] exportByCourse(Integer courseId, ExportStrategy strategy) throws Exception {
        List<schedule> schedules = scheduleRepository.findByCourseId(courseId);
        String title = "📘 Horario del Curso";
        return strategy.export(schedules, title);
    }

    /**
     * Exporta horarios por profesor en el formato especificado.
     *
     * @param teacherId ID del profesor
     * @param strategy Estrategia de exportación a utilizar
     * @return Array de bytes con el contenido exportado
     */
    public byte[] exportByTeacher(Integer teacherId, ExportStrategy strategy) throws Exception {
        List<schedule> schedules = scheduleRepository.findByTeacherId(teacherId);

        // Obtener nombre del profesor para el título
        String teacherName = schedules.stream()
            .filter(s -> s.getTeacherId() != null)
            .map(s -> s.getTeacherId().getTeacherName())
            .findFirst()
            .orElse("Profesor");

        String title = "👨‍🏫 Horario del Profesor: " + teacherName;
        return strategy.export(schedules, title);
    }

    /**
     * Exporta todos los horarios en el formato especificado.
     *
     * @param strategy Estrategia de exportación a utilizar
     * @return Array de bytes con el contenido exportado
     */
    public byte[] exportAllSchedules(ExportStrategy strategy) throws Exception {
        List<schedule> schedules = scheduleRepository.findAll();
        String title = "📚 HORARIO GENERAL - TODOS LOS CURSOS";
        return strategy.export(schedules, title);
    }

    /**
     * Exporta todos los horarios de profesores en el formato especificado.
     *
     * @param strategy Estrategia de exportación a utilizar
     * @return Array de bytes con el contenido exportado
     */
    public byte[] exportAllTeachersSchedules(ExportStrategy strategy) throws Exception {
        List<schedule> schedules = scheduleRepository.findAll();
        String title = "👨‍🏫 HORARIO GENERAL - TODOS LOS PROFESORES";
        return strategy.export(schedules, title);
    }

    // Métodos de conveniencia para formatos específicos

    /**
     * Exporta a PDF usando la estrategia correspondiente.
     */
    public byte[] exportToPdfByCourse(Integer courseId) throws Exception {
        return exportByCourse(courseId, new PdfExportStrategy());
    }

    /**
     * Exporta a Excel usando la estrategia correspondiente.
     */
    public byte[] exportToExcelByCourse(Integer courseId) throws Exception {
        return exportByCourse(courseId, new ExcelExportStrategy());
    }

    /**
     * Exporta a imagen usando la estrategia correspondiente.
     */
    public byte[] exportToImageByCourse(Integer courseId) throws Exception {
        return exportByCourse(courseId, new ImageExportStrategy());
    }

    // Más métodos de conveniencia pueden agregarse aquí según sea necesario
}