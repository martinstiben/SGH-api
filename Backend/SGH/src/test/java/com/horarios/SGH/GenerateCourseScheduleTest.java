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
            
            // Validaciones
            assertEquals("SUCCESS", result.getStatus(), "La generación debe ser exitosa");
            assertTrue(result.getTotalGenerated() > 0, "Debe generar al menos 1 horario para el curso");
            
            System.out.println("\n✅ CURSO 9A: Horario generado exitosamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error generando horario para curso 9A: " + e.getMessage());
            e.printStackTrace();
            fail("Error durante la generación por curso: " + e.getMessage());
        }

        // PASO 3: Probar con otro curso para confirmar que funciona independently
        System.out.println("\n3. Generando horario completo para curso 10A (ID: 11)...");
        
        try {
            ScheduleHistoryDTO result2 = scheduleGenerationService.generateScheduleForCourse(11, "TEST_USER");
            
            System.out.println("=== RESULTADO SEGUNDO CURSO ===");
            System.out.println("Status: " + result2.getStatus());
            System.out.println("Total Generated: " + result2.getTotalGenerated());
            System.out.println("Message: " + result2.getMessage());
            
            assertEquals("SUCCESS", result2.getStatus(), "La segunda generación debe ser exitosa");
            assertTrue(result2.getTotalGenerated() > 0, "Debe generar al menos 1 horario para el segundo curso");
            
            System.out.println("\n✅ CURSO 10A: Horario generado exitosamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error generando horario para curso 10A: " + e.getMessage());
            fail("Error durante la segunda generación: " + e.getMessage());
        }

        System.out.println("\n🎉 TEST COMPLETADO: La generación por curso funciona correctamente!");
        System.out.println("El sistema permite seleccionar un curso específico y generar su horario completo automáticamente.");
    }

    @Test
    public void testGenerateMultipleCoursesIndependently() {
        System.out.println("=== TEST DE GENERACIÓN INDEPENDIENTE DE MÚLTIPLES CURSOS ===\n");

        // Asignar profesores primero
        autoAssignmentService.autoAssignTeachers();
        
        // Cursos a probar
        Integer[] courseIds = {9, 10, 11, 12};
        int totalHorariosGenerados = 0;
        
        for (Integer courseId : courseIds) {
            System.out.println("Generando horario para curso ID: " + courseId);
            
            try {
                ScheduleHistoryDTO result = scheduleGenerationService.generateScheduleForCourse(courseId, "TEST_USER");
                
                assertEquals("SUCCESS", result.getStatus(), "Curso " + courseId + " debe generar exitosamente");
                assertTrue(result.getTotalGenerated() > 0, "Curso " + courseId + " debe generar al menos 1 horario");
                
                totalHorariosGenerados += result.getTotalGenerated();
                System.out.println("✅ Curso " + courseId + ": " + result.getTotalGenerated() + " horarios generados");
                
            } catch (Exception e) {
                System.err.println("❌ Error en curso " + courseId + ": " + e.getMessage());
                fail("Error generando horario para curso " + courseId);
            }
        }
        
        System.out.println("\n📊 RESUMEN FINAL:");
        System.out.println("Total cursos procesados: " + courseIds.length);
        System.out.println("Total horarios generados: " + totalHorariosGenerados);
        System.out.println("Promedio por curso: " + (totalHorariosGenerados / courseIds.length) + " horarios");
        
        assertTrue(totalHorariosGenerados > 0, "Debe generar horarios en total");
        System.out.println("\n✅ TEST EXITOSO: Múltiples cursos pueden generar horarios independientemente");
    }
}