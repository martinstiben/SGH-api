package com.horarios.SGH;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.CourseWithoutAvailabilityDTO;
import com.horarios.SGH.Service.ScheduleGenerationService;
import com.horarios.SGH.Model.*;
import com.horarios.SGH.Repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Clase de prueba para validar la funcionalidad de detección de cursos sin disponibilidad de profesores
 */
@ExtendWith(MockitoExtension.class)
public class ScheduleGenerationServiceAvailabilityTest {

    @Mock
    private IScheduleHistory historyRepository;
    
    @Mock
    private Icourses courseRepo;
    
    @Mock
    private Iteachers teacherRepo;
    
    @Mock
    private ITeacherAvailabilityRepository availabilityRepo;
    
    @Mock
    private IScheduleRepository scheduleRepo;
    
    @Mock
    private Isubjects subjectRepo;
    
    @Mock
    private TeacherSubjectRepository teacherSubjectRepo;
    
    @InjectMocks
    private ScheduleGenerationService scheduleGenerationService;

    @Test
    void testGenerateWithCoursesWithoutAvailability_Success() {
        // Arrange
        ScheduleHistoryDTO request = createValidRequest();
        String executedBy = "test_user";
        
        // Crear entidades de prueba
        teachers teacher = createTestTeacher(1, "Profesor Juan");
        subjects subject = createTestSubject(1, "Matemáticas");
        courses course = createTestCourse(1, "Matemáticas 1A");
        TeacherSubject teacherSubject = createTestTeacherSubject(teacher, subject);
        course.setTeacherSubject(teacherSubject);
        
        // Configurar comportamiento de los repositorios
        when(courseRepo.findAll()).thenReturn(Arrays.asList(course));
        when(scheduleRepo.findByCourseId(any())).thenReturn(Arrays.asList()); // Curso sin horarios
        when(teacherSubjectRepo.findByTeacher_Id(any())).thenReturn(Arrays.asList(teacherSubject));
        when(teacherRepo.findById(any())).thenReturn(java.util.Optional.of(teacher));
        when(subjectRepo.findById(any())).thenReturn(java.util.Optional.of(subject));
        
        // El profesor NO tiene disponibilidad configurada
        when(availabilityRepo.findByTeacher_IdAndDay(eq(1), any(Days.class))).thenReturn(Arrays.asList());
        
        // Simular guardado del historial
        schedule_history history = createTestHistory();
        when(historyRepository.save(any())).thenReturn(history);
        
        // Act
        ScheduleHistoryDTO result = scheduleGenerationService.generate(request, executedBy);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(0, result.getTotalGenerated()); // No se generó ningún horario
        assertNotNull(result.getCoursesWithoutAvailability());
        assertEquals(1, result.getTotalCoursesWithoutAvailability());
        
        // Verificar que se detectó el curso sin disponibilidad
        CourseWithoutAvailabilityDTO courseWithoutAvail = result.getCoursesWithoutAvailability().get(0);
        assertEquals(1, courseWithoutAvail.getCourseId());
        assertEquals("Matemáticas 1A", courseWithoutAvail.getCourseName());
        assertEquals(1, courseWithoutAvail.getTeacherId());
        assertEquals("Profesor Juan", courseWithoutAvail.getTeacherName());
        assertEquals("NO_AVAILABILITY_DEFINED", courseWithoutAvail.getReason());
        assertTrue(courseWithoutAvail.getDescription().contains("no tiene disponibilidad configurada"));
        
        // Verificar el mensaje de éxito incluye información sobre cursos sin disponibilidad
        assertTrue(result.getMessage().contains("cursos sin disponibilidad"));
    }

    @Test
    void testGenerateWithTeacherHavingConflicts_Success() {
        // Arrange
        ScheduleHistoryDTO request = createValidRequest();
        String executedBy = "test_user";
        
        // Crear entidades de prueba
        teachers teacher = createTestTeacher(1, "Profesor Juan");
        subjects subject = createTestSubject(1, "Matemáticas");
        courses course = createTestCourse(1, "Matemáticas 1A");
        TeacherSubject teacherSubject = createTestTeacherSubject(teacher, subject);
        course.setTeacherSubject(teacherSubject);
        
        // Crear disponibilidad válida para el profesor
        TeacherAvailability availability = createTestAvailability(teacher, Days.Lunes, 
            java.time.LocalTime.of(8, 0), java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(13, 0), java.time.LocalTime.of(17, 0));
        
        // Configurar comportamiento de los repositorios
        when(courseRepo.findAll()).thenReturn(Arrays.asList(course));
        when(scheduleRepo.findByCourseId(any())).thenReturn(Arrays.asList()); // Curso sin horarios
        when(scheduleRepo.findByTeacherId(any())).thenReturn(createExistingSchedules(teacher, "Lunes")); // Conflicto existente
        when(teacherSubjectRepo.findByTeacher_Id(any())).thenReturn(Arrays.asList(teacherSubject));
        when(teacherRepo.findById(any())).thenReturn(java.util.Optional.of(teacher));
        when(subjectRepo.findById(any())).thenReturn(java.util.Optional.of(subject));
        when(availabilityRepo.findByTeacher_IdAndDay(eq(1), eq(Days.Lunes))).thenReturn(Arrays.asList(availability));
        
        // Simular guardado del historial
        schedule_history history = createTestHistory();
        when(historyRepository.save(any())).thenReturn(history);
        
        // Act
        ScheduleHistoryDTO result = scheduleGenerationService.generate(request, executedBy);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(0, result.getTotalGenerated());
        assertNotNull(result.getCoursesWithoutAvailability());
        assertEquals(1, result.getTotalCoursesWithoutAvailability());
        
        // Verificar que se detectó el conflicto
        CourseWithoutAvailabilityDTO courseWithoutAvail = result.getCoursesWithoutAvailability().get(0);
        assertEquals("CONFLICTS_WITH_EXISTING", courseWithoutAvail.getReason());
        assertTrue(courseWithoutAvail.getDescription().contains("tiene conflictos de horario"));
    }

    @Test
    void testGenerateWithMixedResults_Success() {
        // Arrange
        ScheduleHistoryDTO request = createValidRequest();
        String executedBy = "test_user";
        
        // Crear múltiples cursos: uno sin disponibilidad, otro asignable
        teachers teacher1 = createTestTeacher(1, "Profesor Juan");
        teachers teacher2 = createTestTeacher(2, "Profesora María");
        
        subjects subject1 = createTestSubject(1, "Matemáticas");
        subjects subject2 = createTestSubject(2, "Ciencias");
        
        courses course1 = createTestCourse(1, "Matemáticas 1A");
        courses course2 = createTestCourse(2, "Ciencias 1B");
        
        TeacherSubject teacherSubject1 = createTestTeacherSubject(teacher1, subject1);
        TeacherSubject teacherSubject2 = createTestTeacherSubject(teacher2, subject2);
        
        course1.setTeacherSubject(teacherSubject1);
        course2.setTeacherSubject(teacherSubject2);
        
        // Crear disponibilidad para el segundo profesor
        TeacherAvailability availability = createTestAvailability(teacher2, Days.Lunes, 
            java.time.LocalTime.of(8, 0), java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(13, 0), java.time.LocalTime.of(17, 0));
        
        // Configurar comportamiento de los repositorios
        when(courseRepo.findAll()).thenReturn(Arrays.asList(course1, course2));
        when(scheduleRepo.findByCourseId(any())).thenReturn(Arrays.asList()); // Ambos sin horarios
        
        // Profesor 1 sin disponibilidad, Profesor 2 con disponibilidad
        when(availabilityRepo.findByTeacher_IdAndDay(eq(1), any(Days.class))).thenReturn(Arrays.asList());
        when(availabilityRepo.findByTeacher_IdAndDay(eq(2), eq(Days.Lunes))).thenReturn(Arrays.asList(availability));
        
        when(teacherSubjectRepo.findByTeacher_Id(eq(1))).thenReturn(Arrays.asList(teacherSubject1));
        when(teacherSubjectRepo.findByTeacher_Id(eq(2))).thenReturn(Arrays.asList(teacherSubject2));
        
        when(teacherRepo.findById(eq(1))).thenReturn(java.util.Optional.of(teacher1));
        when(teacherRepo.findById(eq(2))).thenReturn(java.util.Optional.of(teacher2));
        when(subjectRepo.findById(eq(1))).thenReturn(java.util.Optional.of(subject1));
        when(subjectRepo.findById(eq(2))).thenReturn(java.util.Optional.of(subject2));
        
        when(scheduleRepo.findByTeacherId(any())).thenReturn(Arrays.asList()); // Sin conflictos
        
        // Simular guardado del historial
        schedule_history history = createTestHistory();
        when(historyRepository.save(any())).thenReturn(history);
        
        // Act
        ScheduleHistoryDTO result = scheduleGenerationService.generate(request, executedBy);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1, result.getTotalGenerated()); // Un curso se generó exitosamente
        assertNotNull(result.getCoursesWithoutAvailability());
        assertEquals(1, result.getTotalCoursesWithoutAvailability()); // Un curso sin disponibilidad
        
        // Verificar que el curso sin disponibilidad es el correcto
        CourseWithoutAvailabilityDTO courseWithoutAvail = result.getCoursesWithoutAvailability().get(0);
        assertEquals(1, courseWithoutAvail.getCourseId());
        assertEquals("NO_AVAILABILITY_DEFINED", courseWithoutAvail.getReason());
    }

    private ScheduleHistoryDTO createValidRequest() {
        ScheduleHistoryDTO dto = new ScheduleHistoryDTO();
        dto.setPeriodStart(LocalDate.now());
        dto.setPeriodEnd(LocalDate.now().plusDays(5));
        dto.setDryRun(false);
        dto.setForce(false);
        dto.setParams("Test generation");
        return dto;
    }

    private teachers createTestTeacher(Integer id, String name) {
        teachers teacher = new teachers();
        teacher.setId(id);
        teacher.setTeacherName(name);
        return teacher;
    }

    private subjects createTestSubject(Integer id, String name) {
        subjects subject = new subjects();
        subject.setId(id);
        subject.setSubjectName(name);
        return subject;
    }

    private courses createTestCourse(Integer id, String name) {
        courses course = new courses();
        course.setId(id);
        course.setCourseName(name);
        return course;
    }

    private TeacherSubject createTestTeacherSubject(teachers teacher, subjects subject) {
        TeacherSubject ts = new TeacherSubject();
        ts.setTeacher(teacher);
        ts.setSubject(subject);
        return ts;
    }

    private TeacherAvailability createTestAvailability(teachers teacher, Days day,
        java.time.LocalTime amStart, java.time.LocalTime amEnd,
        java.time.LocalTime pmStart, java.time.LocalTime pmEnd) {
        TeacherAvailability availability = new TeacherAvailability();
        availability.setTeacher(teacher);
        availability.setDay(day);
        availability.setAmStart(amStart);
        availability.setAmEnd(amEnd);
        availability.setPmStart(pmStart);
        availability.setPmEnd(pmEnd);
        return availability;
    }

    private List<schedule> createExistingSchedules(teachers teacher, String day) {
        schedule existing = new schedule();
        existing.setTeacherId(teacher);
        existing.setDay(day);
        existing.setStartTime(java.time.LocalTime.of(9, 0));
        existing.setEndTime(java.time.LocalTime.of(10, 0));
        return Arrays.asList(existing);
    }

    private schedule_history createTestHistory() {
        schedule_history history = new schedule_history();
        history.setId(1);
        history.setStatus("SUCCESS");
        history.setTotalGenerated(0);
        history.setMessage("Test message");
        return history;
    }
}