package com.horarios.SGH;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.CourseWithoutAvailabilityDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración para validar la funcionalidad de detección de cursos sin disponibilidad
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScheduleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/schedules";
    }

    @Test
    public void testGenerateSchedule_ReturnsCoursesWithoutAvailability() {
        // Arrange
        ScheduleHistoryDTO request = new ScheduleHistoryDTO();
        request.setPeriodStart(java.time.LocalDate.of(2025, 1, 20));
        request.setPeriodEnd(java.time.LocalDate.of(2025, 1, 24));
        request.setDryRun(false);
        request.setForce(false);
        request.setParams("Test de detección de disponibilidad");

        // Act
        ResponseEntity<ScheduleHistoryDTO> response = restTemplate.postForEntity(
            baseUrl() + "/generate", request, ScheduleHistoryDTO.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        ScheduleHistoryDTO result = response.getBody();
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getCoursesWithoutAvailability());
        assertTrue(result.getTotalCoursesWithoutAvailability() >= 0);
        
        // Verificar que los campos de cursos sin disponibilidad están presentes
        if (!result.getCoursesWithoutAvailability().isEmpty()) {
            for (CourseWithoutAvailabilityDTO course : result.getCoursesWithoutAvailability()) {
                assertNotNull(course.getCourseId());
                assertNotNull(course.getCourseName());
                assertNotNull(course.getReason());
                assertNotNull(course.getDescription());
            }
        }
    }

    @Test
    public void testGenerateSchedule_WithDryRunMode() {
        // Arrange
        ScheduleHistoryDTO request = new ScheduleHistoryDTO();
        request.setPeriodStart(java.time.LocalDate.of(2025, 1, 20));
        request.setPeriodEnd(java.time.LocalDate.of(2025, 1, 24));
        request.setDryRun(true); // Modo simulación
        request.setForce(false);
        request.setParams("Simulación de detección");

        // Act
        ResponseEntity<ScheduleHistoryDTO> response = restTemplate.postForEntity(
            baseUrl() + "/generate", request, ScheduleHistoryDTO.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        ScheduleHistoryDTO result = response.getBody();
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getCoursesWithoutAvailability());
    }
}