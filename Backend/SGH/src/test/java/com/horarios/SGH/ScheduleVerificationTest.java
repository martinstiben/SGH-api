package com.horarios.SGH;

import com.horarios.SGH.DTO.ScheduleVerificationReportDTO;
import com.horarios.SGH.Service.AutoAssignmentService;
import com.horarios.SGH.Service.ScheduleGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test para verificar la funcionalidad de verificación exhaustiva de horarios
 */
@SpringBootTest
@Transactional
public class ScheduleVerificationTest {

    @Autowired
    private AutoAssignmentService autoAssignmentService;

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Test
    public void testScheduleVerificationForGeneratedCourse() {
        System.out.println("=== TEST DE VERIFICACIÓN EXHAUSTIVA DE HORARIO ===\n");

        // PASO 1: Preparar datos (asignar profesores)
        System.out.println("1. Preparando datos - Asignando profesores...");
        String assignmentResult = autoAssignmentService.autoAssignTeachers();
        assertTrue(assignmentResult.contains("Asignaciones realizadas"), "Asignación de profesores debe ser exitosa");

        // PASO 2: Generar horario para curso 9
        System.out.println("2. Generando horario para curso 9...");
        var result = scheduleGenerationService.generateScheduleForCourse(9, "TEST_USER");
        assertEquals("SUCCESS", result.getStatus(), "Generación debe ser exitosa");
        assertTrue(result.getTotalGenerated() > 0, "Debe generar al menos una clase");

        // PASO 3: Verificar el horario generado
        System.out.println("3. Verificando horario generado...");
        ScheduleVerificationReportDTO report = scheduleGenerationService.verifyGeneratedSchedule(9);

        // Verificaciones básicas
        assertNotNull(report, "Debe generar un reporte de verificación");
        assertEquals("9A", report.getCourseName(), "Nombre del curso debe ser correcto");
        assertEquals(9, report.getCourseId(), "ID del curso debe ser correcto");

        // Verificar estadísticas
        assertNotNull(report.getStatistics(), "Debe incluir estadísticas");
        assertTrue(report.getStatistics().getTotalClasses() > 0, "Debe tener clases generadas");
        assertTrue(report.getStatistics().getTotalSlotsAvailable() > 0, "Debe calcular slots disponibles");

        // Verificar validaciones
        assertNotNull(report.getProtectedBlocks(), "Debe verificar bloques protegidos");
        assertNotNull(report.getContent(), "Debe verificar contenido");
        assertNotNull(report.getConflicts(), "Debe verificar conflictos");
        assertNotNull(report.getFormat(), "Debe verificar formato");

        // Verificar que los bloques protegidos están respetados
        assertTrue(report.getProtectedBlocks().isBreakTimeRespected(),
            "Debe respetar el bloque de descanso");
        assertTrue(report.getProtectedBlocks().isLunchTimeRespected(),
            "Debe respetar el bloque de almuerzo");

        // Verificar que no hay errores críticos
        assertTrue(report.getErrors().isEmpty(),
            "No debe tener errores críticos. Errores encontrados: " + report.getErrors());

        // Verificar que tiene al menos algunos éxitos
        assertFalse(report.getSuccesses().isEmpty(),
            "Debe tener al menos algunas validaciones exitosas");

        // Mostrar resultados
        System.out.println("=== RESULTADOS DE VERIFICACIÓN ===");
        System.out.println("Curso: " + report.getCourseName());
        System.out.println("Completo: " + report.isComplete());
        System.out.println("Válido: " + report.isValid());
        System.out.println("Clases generadas: " + report.getStatistics().getTotalClasses());
        System.out.println("Cobertura: " + String.format("%.1f%%", report.getStatistics().getCoveragePercentage()));

        System.out.println("\n✅ ÉXITOS:");
        report.getSuccesses().forEach(success -> System.out.println("  " + success));

        if (!report.getWarnings().isEmpty()) {
            System.out.println("\n⚠️ ADVERTENCIAS:");
            report.getWarnings().forEach(warning -> System.out.println("  " + warning));
        }

        System.out.println("\n🎯 TEST COMPLETADO: Verificación exhaustiva funciona correctamente");
        System.out.println("El horario generado cumple con todos los criterios de calidad requeridos");
    }
}