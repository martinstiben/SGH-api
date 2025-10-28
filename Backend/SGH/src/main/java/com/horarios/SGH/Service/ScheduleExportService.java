package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Repository.IScheduleRepository;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

/**
 * Servicio de exportación de horarios - Versión Legacy.
 * Mantenido por compatibilidad. Se recomienda usar ScheduleExportServiceRefactored.
 *
 * @deprecated Use ScheduleExportServiceRefactored instead for better SOLID compliance
 */
@Service
@RequiredArgsConstructor
@Deprecated
public class ScheduleExportService {

    private final IScheduleRepository scheduleRepository;
    private final ScheduleExportServiceRefactored refactoredService;

    private List<String> generateTimes(List<schedule> schedules) {
        Set<String> timeSet = new TreeSet<>();
        // Always include break times first
        timeSet.add("09:00");
        timeSet.add("12:00");
        for (schedule s : schedules) {
            String startTime = s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            // Exclude schedules that coincide with break times
            if (!startTime.equals("09:00") && !startTime.equals("12:00")) {
                timeSet.add(startTime);
            }
        }
        List<String> times = new java.util.ArrayList<>();
        for (String startTime : timeSet) {
            String[] parts = startTime.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int endHours = hours;
            int endMinutes = minutes;
            if (startTime.equals("09:00")) {
                // Descanso de 30 minutos
                endMinutes += 30;
            } else {
                // Clases de 1 hora
                endHours += 1;
            }
            String endTime = String.format("%02d:%02d", endHours, endMinutes);
            String periodStart = formatTime(startTime);
            String periodEnd = formatTime(endTime);
            times.add(periodStart + " - " + periodEnd);
        }
        return times;
    }

    private String formatTime(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        String period = hours >= 12 ? "PM" : "AM";
        int displayHours = hours % 12;
        if (displayHours == 0) displayHours = 12;
        return String.format("%d:%02d %s", displayHours, minutes, period);
    }

    private schedule getScheduleForTimeAndDay(List<schedule> schedules, String time, String day) {
        String[] timeParts = time.split(" - ");
        String startTimeStr = timeParts[0];
        String[] hmp = startTimeStr.split("[: ]");
        int hours = Integer.parseInt(hmp[0]);
        if (hmp[2].equals("PM") && hours != 12) hours += 12;
        if (hmp[2].equals("AM") && hours == 12) hours = 0;
        String scheduleTime = String.format("%02d:%s", hours, hmp[1]);

        for (schedule s : schedules) {
            if (s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")).equals(scheduleTime) && s.getDay().equals(day)) {
                return s;
            }
        }
        return null;
    }

    public byte[] exportToPdfByCourse(Integer courseId) throws Exception {
        return refactoredService.exportToPdfByCourse(courseId);
    }

    public byte[] exportToPdfByTeacher(Integer teacherId) throws Exception {
        return refactoredService.exportByTeacher(teacherId, new PdfExportStrategy());
    }

    public byte[] exportToExcelByCourse(Integer courseId) throws Exception {
        return refactoredService.exportToExcelByCourse(courseId);
    }

    public byte[] exportToExcelByTeacher(Integer teacherId) throws Exception {
        return refactoredService.exportByTeacher(teacherId, new ExcelExportStrategy());
    }

    public byte[] exportToImageByCourse(Integer courseId) throws Exception {
        return refactoredService.exportToImageByCourse(courseId);
    }

    public byte[] exportToImageByTeacher(Integer teacherId) throws Exception {
        return refactoredService.exportByTeacher(teacherId, new ImageExportStrategy());
    }

    public byte[] exportToPdfAllSchedules() throws Exception {
        return refactoredService.exportAllSchedules(new PdfExportStrategy());
    }

    public byte[] exportToPdfAllTeachersSchedules() throws Exception {
        return refactoredService.exportAllTeachersSchedules(new PdfExportStrategy());
    }

    public byte[] exportToExcelAllSchedules() throws Exception {
        return refactoredService.exportAllSchedules(new ExcelExportStrategy());
    }

    public byte[] exportToExcelAllTeachersSchedules() throws Exception {
        return refactoredService.exportAllTeachersSchedules(new ExcelExportStrategy());
    }

    public byte[] exportToImageAllSchedules() throws Exception {
        return refactoredService.exportAllSchedules(new ImageExportStrategy());
    }

    public byte[] exportToImageAllTeachersSchedules() throws Exception {
        return refactoredService.exportAllTeachersSchedules(new ImageExportStrategy());
    }
}