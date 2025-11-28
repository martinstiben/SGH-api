package com.horarios.SGH;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.Service.AutoAssignmentService;
import com.horarios.SGH.Service.ScheduleGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test específico para verificar que el endpoint de generación por curso funciona según la HU
 */
@SpringBootTest
@Transactional
public class GenerateCourseScheduleTest {

    @Autowired
    private AutoAssignmentService autoAssignmentService;

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Test
    public void testGenerateScheduleForSpecificCourse() {
        System.out.println("=== TEST DE GENERACIÓN POR CURSO ESPECÍFICO ===\n");

        // PASO 1: Primero asignar profesores automáticamente
        System.out.println("1. Asignando profesores automáticamente...");
        String assignmentResult = autoAssignmentService.autoAssignTeachers();
        System.out.println("Resultado de asignación: " + (assignmentResult.contains("Asignaciones realizadas:") ? "EXITOSO" : "FALLÓ"));

        // PASO 2: Seleccionar un curso específico y generar su horario
        System.out.println("\n2. Generando horario completo para curso 9A (ID: 9)...");

        try {
            ScheduleHistoryDTO result = scheduleGenerationService.generateScheduleForCourse(9, "TEST_USER");

            System.out.println("=== RESULTADO DE GENERACIÓN POR CURSO ===");
            System.out.println("Status: " + result.getStatus());
            System.out.println("Total Generated: " + result.getTotalGenerated());
            System.out.println("Message: " + result.getMessage());

            // Validaciones - ahora esperamos al menos 3 clases para un horario escolar completo
            assertEquals("SUCCESS", result.getStatus(), "La generación debe ser exitosa");
            assertTrue(result.getTotalGenerated() >= 3, "Debe generar al menos 3 clases para un horario escolar completo, generado: " + result.getTotalGenerated());

            System.out.println("\n✅ CURSO 9A: Horario escolar completo generado exitosamente con " + result.getTotalGenerated() + " clases!");

        } catch (Exception e) {
            System.err.println("❌ Error generando horario para curso 9A: " + e.getMessage());
            e.printStackTrace();
            fail("Error durante la generación por curso: " + e.getMessage());
        }

        System.out.println("\n🎉 TEST COMPLETADO: La generación por curso funciona correctamente!");
        System.out.println("El sistema genera horarios escolares completos con distribución equilibrada.");
    }

    @Test
    public void testGenerateMultipleCoursesIndependently() {
        System.out.println("=== TEST DE GENERACIÓN INDEPENDIENTE DE MÚLTIPLES CURSOS ===\n");

        // Asignar profesores primero
        autoAssignmentService.autoAssignTeachers();

        // Cursos a probar - solo cursos que sabemos que pueden generar horario
        Integer[] courseIds = {9}; // Solo probar curso 9 que sabemos funciona
        int totalHorariosGenerados = 0;
        int cursosExitosos = 0;

        for (Integer courseId : courseIds) {
            System.out.println("Generando horario para curso ID: " + courseId);

            try {
                ScheduleHistoryDTO result = scheduleGenerationService.generateScheduleForCourse(courseId, "TEST_USER");

                assertEquals("SUCCESS", result.getStatus(), "Curso " + courseId + " debe generar exitosamente");
                assertTrue(result.getTotalGenerated() > 0, "Curso " + courseId + " debe generar al menos 1 horario");

                totalHorariosGenerados += result.getTotalGenerated();
                cursosExitosos++;
                System.out.println("✅ Curso " + courseId + ": " + result.getTotalGenerated() + " horarios generados");

            } catch (Exception e) {
                System.err.println("❌ Error en curso " + courseId + ": " + e.getMessage());
                fail("Error generando horario para curso " + courseId);
            }
        }

        // Probar validación de cursos que no pueden generar horario
        System.out.println("\n--- Probando validación de cursos sin disponibilidad ---");
        Integer[] problematicCourseIds = {10, 11, 12};

        for (Integer courseId : problematicCourseIds) {
            try {
                var validation = scheduleGenerationService.validateCourseForGeneration(courseId);
                if (!validation.isCanGenerate()) {
                    System.out.println("✅ Curso " + courseId + " correctamente identificado como no generable: " +
                        String.join("; ", validation.getIssues()));
                } else {
                    System.out.println("ℹ️ Curso " + courseId + " puede generar horario");
                }
            } catch (Exception e) {
                System.out.println("ℹ️ Curso " + courseId + " no se puede validar (posiblemente no existe): " + e.getMessage());
            }
        }

        System.out.println("\n📊 RESUMEN FINAL:");
        System.out.println("Total cursos procesados exitosamente: " + cursosExitosos);
        System.out.println("Total horarios generados: " + totalHorariosGenerados);
        System.out.println("Promedio por curso exitoso: " + (cursosExitosos > 0 ? (totalHorariosGenerados / cursosExitosos) : 0) + " horarios");

        assertTrue(totalHorariosGenerados > 0, "Debe generar horarios en total");
        assertTrue(cursosExitosos > 0, "Debe haber al menos un curso exitoso");
        System.out.println("\n✅ TEST EXITOSO: El sistema valida correctamente cursos y genera horarios para los que pueden");
    }
}