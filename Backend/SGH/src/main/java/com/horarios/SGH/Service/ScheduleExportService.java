package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Repository.IScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de exportación de horarios académicos.
 * Proporciona funcionalidades para exportar horarios en diferentes formatos (PDF, Excel, Imagen).
 * 
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de exportación de horarios
 * - OCP: Abierto para extensión mediante nuevas estrategias
 * - LSP: Implementaciones sustituibles
 * - ISP: Interfaces específicas para diferentes formatos
 * - DIP: Depende de abstracciones (repositorios, estrategias)
 * 
 * Funcionalidades:
 * - Exportación por curso
 * - Exportación por profesor
 * - Exportación general de todos los horarios
 * - Múltiples formatos de salida (PDF, Excel, Imagen)
 * 
 * @author Sistema SGH
 * @version 2.0 - Refactorizado y funcional
 */
@Service
public class ScheduleExportService {

    private final IScheduleRepository scheduleRepository;
    private final PdfExportStrategy pdfExportStrategy;
    private final ExcelExportStrategy excelExportStrategy;
    private final ImageExportStrategy imageExportStrategy;
    
    /**
     * Constructor manual para inyección de dependencias.
     * Mantiene compatibilidad con Spring y permite testing.
     *
     * @param scheduleRepository Repositorio de horarios
     * @param pdfExportStrategy Estrategia de exportación a PDF
     * @param excelExportStrategy Estrategia de exportación a Excel
     * @param imageExportStrategy Estrategia de exportación a imagen
     */
    public ScheduleExportService(IScheduleRepository scheduleRepository,
                                PdfExportStrategy pdfExportStrategy,
                                ExcelExportStrategy excelExportStrategy,
                                ImageExportStrategy imageExportStrategy) {
        this.scheduleRepository = scheduleRepository;
        this.pdfExportStrategy = pdfExportStrategy;
        this.excelExportStrategy = excelExportStrategy;
        this.imageExportStrategy = imageExportStrategy;
    }

    /**
     * Valida que el ID del curso sea válido.
     *
     * @param courseId ID del curso a validar
     * @throws IllegalArgumentException si el courseId es inválido
     */
    private void validateCourseId(Integer courseId) {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }
    }
    
    /**
     * Valida que el ID del profesor sea válido.
     *
     * @param teacherId ID del profesor a validar
     * @throws IllegalArgumentException si el teacherId es inválido
     */
    private void validateTeacherId(Integer teacherId) {
        if (teacherId == null || teacherId <= 0) {
            throw new IllegalArgumentException("El ID del profesor debe ser un número positivo");
        }
    }
    
    /**
     * Obtiene los horarios por curso con validación.
     *
     * @param courseId ID del curso
     * @return Lista de horarios del curso
     * @throws IllegalArgumentException si el courseId es inválido
     */
    private List<schedule> getSchedulesByCourse(Integer courseId) {
        validateCourseId(courseId);
        List<schedule> schedules = scheduleRepository.findByCourseId(courseId);
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron horarios para el curso con ID: " + courseId);
        }
        return schedules;
    }
    
    /**
     * Obtiene los horarios por profesor con validación.
     *
     * @param teacherId ID del profesor
     * @return Lista de horarios del profesor
     * @throws IllegalArgumentException si el teacherId es inválido
     */
    private List<schedule> getSchedulesByTeacher(Integer teacherId) {
        validateTeacherId(teacherId);
        List<schedule> schedules = scheduleRepository.findByTeacherId(teacherId);
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron horarios para el profesor con ID: " + teacherId);
        }
        return schedules;
    }
    
    /**
     * Obtiene todos los horarios del sistema.
     *
     * @return Lista de todos los horarios
     * @throws IllegalStateException si no hay horarios en el sistema
     */
    private List<schedule> getAllSchedules() {
        List<schedule> schedules = scheduleRepository.findAll();
        if (schedules.isEmpty()) {
            throw new IllegalStateException("No hay horarios registrados en el sistema");
        }
        return schedules;
    }

    /**
     * Exporta los horarios de un curso específico a formato PDF.
     *
     * @param courseId ID del curso a exportar
     * @return Array de bytes con el contenido del archivo PDF
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToPdfByCourse(Integer courseId) throws Exception {
        List<schedule> schedules = getSchedulesByCourse(courseId);
        return pdfExportStrategy.export(schedules, "📘 Horario del Curso");
    }

    /**
     * Exporta los horarios de un profesor específico a formato PDF.
     *
     * @param teacherId ID del profesor a exportar
     * @return Array de bytes con el contenido del archivo PDF
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToPdfByTeacher(Integer teacherId) throws Exception {
        List<schedule> schedules = getSchedulesByTeacher(teacherId);
        
        // Obtener nombre del profesor para el título
        String teacherName = schedules.stream()
            .filter(s -> s.getTeacherId() != null)
            .map(s -> s.getTeacherId().getTeacherName())
            .findFirst()
            .orElse("Profesor");
            
        return pdfExportStrategy.export(schedules, "👨‍🏫 Horario del Profesor: " + teacherName);
    }

    /**
     * Exporta los horarios de un curso específico a formato Excel.
     *
     * @param courseId ID del curso a exportar
     * @return Array de bytes con el contenido del archivo Excel
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToExcelByCourse(Integer courseId) throws Exception {
        List<schedule> schedules = getSchedulesByCourse(courseId);
        return excelExportStrategy.export(schedules, "📘 Horario del Curso");
    }

    /**
     * Exporta los horarios de un profesor específico a formato Excel.
     *
     * @param teacherId ID del profesor a exportar
     * @return Array de bytes con el contenido del archivo Excel
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToExcelByTeacher(Integer teacherId) throws Exception {
        List<schedule> schedules = getSchedulesByTeacher(teacherId);
        
        // Obtener nombre del profesor para el título
        String teacherName = schedules.stream()
            .filter(s -> s.getTeacherId() != null)
            .map(s -> s.getTeacherId().getTeacherName())
            .findFirst()
            .orElse("Profesor");
            
        return excelExportStrategy.export(schedules, "👨‍🏫 Horario del Profesor: " + teacherName);
    }

    /**
     * Exporta los horarios de un curso específico a formato de imagen.
     *
     * @param courseId ID del curso a exportar
     * @return Array de bytes con el contenido de la imagen
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToImageByCourse(Integer courseId) throws Exception {
        List<schedule> schedules = getSchedulesByCourse(courseId);
        return imageExportStrategy.export(schedules, "📘 Horario del Curso");
    }

    /**
     * Exporta los horarios de un profesor específico a formato de imagen.
     *
     * @param teacherId ID del profesor a exportar
     * @return Array de bytes con el contenido de la imagen
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToImageByTeacher(Integer teacherId) throws Exception {
        List<schedule> schedules = getSchedulesByTeacher(teacherId);
        
        // Obtener nombre del profesor para el título
        String teacherName = schedules.stream()
            .filter(s -> s.getTeacherId() != null)
            .map(s -> s.getTeacherId().getTeacherName())
            .findFirst()
            .orElse("Profesor");
            
        return imageExportStrategy.export(schedules, "👨‍🏫 Horario del Profesor: " + teacherName);
    }

    /**
     * Exporta todos los horarios del sistema a formato PDF.
     *
     * @return Array de bytes con el contenido del archivo PDF
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToPdfAllSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return pdfExportStrategy.export(schedules, "📚 HORARIO GENERAL - TODOS LOS CURSOS");
    }

    /**
     * Exporta todos los horarios de profesores a formato PDF.
     *
     * @return Array de bytes con el contenido del archivo PDF
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToPdfAllTeachersSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return pdfExportStrategy.export(schedules, "👨‍🏫 HORARIO GENERAL - TODOS LOS PROFESORES");
    }

    /**
     * Exporta todos los horarios del sistema a formato Excel.
     *
     * @return Array de bytes con el contenido del archivo Excel
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToExcelAllSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return excelExportStrategy.export(schedules, "📚 HORARIO GENERAL - TODOS LOS CURSOS");
    }

    /**
     * Exporta todos los horarios de profesores a formato Excel.
     *
     * @return Array de bytes con el contenido del archivo Excel
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToExcelAllTeachersSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return excelExportStrategy.export(schedules, "👨‍🏫 HORARIO GENERAL - TODOS LOS PROFESORES");
    }

    /**
     * Exporta todos los horarios del sistema a formato de imagen.
     *
     * @return Array de bytes con el contenido de la imagen
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToImageAllSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return imageExportStrategy.export(schedules, "📚 HORARIO GENERAL - TODOS LOS CURSOS");
    }

    /**
     * Exporta todos los horarios de profesores a formato de imagen.
     *
     * @return Array de bytes con el contenido de la imagen
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportToImageAllTeachersSchedules() throws Exception {
        List<schedule> schedules = getAllSchedules();
        return imageExportStrategy.export(schedules, "👨‍🏫 HORARIO GENERAL - TODOS LOS PROFESORES");
    }
    
    // Métodos adicionales con estrategias específicas
    
    /**
     * Exporta horarios por curso usando una estrategia de exportación específica.
     *
     * @param courseId ID del curso del cual exportar horarios
     * @param strategy Estrategia de exportación a utilizar (PDF, Excel, Image, etc.)
     * @return Array de bytes con el contenido exportado en el formato especificado
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportByCourse(Integer courseId, ExportStrategy strategy) throws Exception {
        if (strategy == null) {
            throw new IllegalArgumentException("La estrategia de exportación no puede ser null");
        }
        
        List<schedule> schedules = getSchedulesByCourse(courseId);
        String title = "📘 Horario del Curso";
        return strategy.export(schedules, title);
    }
    
    /**
     * Exporta horarios por profesor usando una estrategia de exportación específica.
     *
     * @param teacherId ID del profesor del cual exportar horarios
     * @param strategy Estrategia de exportación a utilizar (PDF, Excel, Image, etc.)
     * @return Array de bytes con el contenido exportado en el formato especificado
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportByTeacher(Integer teacherId, ExportStrategy strategy) throws Exception {
        if (strategy == null) {
            throw new IllegalArgumentException("La estrategia de exportación no puede ser null");
        }
        
        List<schedule> schedules = getSchedulesByTeacher(teacherId);
        
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
     * Exporta todos los horarios del sistema usando una estrategia específica.
     * Incluye horarios de todos los cursos y profesores.
     *
     * @param strategy Estrategia de exportación a utilizar (PDF, Excel, Image, etc.)
     * @return Array de bytes con el contenido exportado en el formato especificado
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportAllSchedules(ExportStrategy strategy) throws Exception {
        if (strategy == null) {
            throw new IllegalArgumentException("La estrategia de exportación no puede ser null");
        }
        
        List<schedule> schedules = getAllSchedules();
        String title = "📚 HORARIO GENERAL - TODOS LOS CURSOS";
        return strategy.export(schedules, title);
    }
    
    /**
     * Exporta todos los horarios de profesores usando una estrategia específica.
     * Vista general de todos los horarios desde la perspectiva de profesores.
     *
     * @param strategy Estrategia de exportación a utilizar (PDF, Excel, Image, etc.)
     * @return Array de bytes con el contenido exportado en el formato especificado
     * @throws Exception si ocurre un error durante la exportación
     */
    public byte[] exportAllTeachersSchedules(ExportStrategy strategy) throws Exception {
        if (strategy == null) {
            throw new IllegalArgumentException("La estrategia de exportación no puede ser null");
        }
        
        List<schedule> schedules = getAllSchedules();
        String title = "👨‍🏫 HORARIO GENERAL - TODOS LOS PROFESORES";
        return strategy.export(schedules, title);
    }
}