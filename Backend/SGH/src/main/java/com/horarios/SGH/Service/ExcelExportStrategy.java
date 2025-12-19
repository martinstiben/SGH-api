package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Estrategia de exportación a Excel.
 * Implementa la interfaz ExportStrategy para exportar horarios en formato Excel.
 */
public class ExcelExportStrategy implements ExportStrategy {

    @Override
    public byte[] export(List<schedule> schedules, String title) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Horario");

        // Estilos
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Agregar título
        addTitle(sheet, title, headerStyle);

        // Crear tabla de horarios
        createScheduleTable(sheet, schedules, headerStyle);

        // Auto-ajustar columnas
        autoSizeColumns(sheet);

        // Convertir a bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private void addTitle(Sheet sheet, String title, CellStyle headerStyle) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
    }

    private void createScheduleTable(Sheet sheet, List<schedule> schedules, CellStyle headerStyle) {
        String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] dayKeys = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        List<String> times = generateTimes(schedules);

        // Header
        Row headerRow = sheet.createRow(2);
        headerRow.createCell(0).setCellValue("Tiempo");
        for (int i = 0; i < dayNames.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(dayNames[i]);
            cell.setCellStyle(headerStyle);
        }

        // Contenido
        int rowIdx = 3;
        for (String time : times) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(time);

            for (int i = 0; i < dayKeys.length; i++) {
                String day = dayKeys[i];
                schedule s = getScheduleForTimeAndDay(schedules, time, day);
                String content = getScheduleContent(s, time);
                row.createCell(i + 1).setCellValue(content);
            }
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i <= 6; i++) { // 0=Tiempo + 5 días + 1 extra
            sheet.autoSizeColumn(i);
        }
    }

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
            if (s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")).equals(scheduleTime) && s.getDay().equalsIgnoreCase(day)) {
                return s;
            }
        }
        return null;
    }

    private String getScheduleContent(schedule s, String time) {
        if (time.equals("9:00 AM - 9:30 AM")) {
            return "Descanso";
        } else if (time.equals("12:00 PM - 1:00 PM")) {
            return "Almuerzo";
        } else if (s != null) {
            String docente = (s.getTeacherId() != null && s.getTeacherId().getTeacherName() != null) ? s.getTeacherId().getTeacherName() : "";
            String materia = (s.getSubjectId() != null && s.getSubjectId().getSubjectName() != null) ? s.getSubjectId().getSubjectName() : "";
            if (!docente.isEmpty() && !materia.isEmpty()) {
                return docente + " - " + materia;
            } else if (!docente.isEmpty()) {
                return docente;
            } else if (!materia.isEmpty()) {
                return materia;
            }
        }
        return "";
    }
}