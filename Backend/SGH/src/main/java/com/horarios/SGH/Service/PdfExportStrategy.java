package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Estrategia de exportación a PDF.
 * Implementa la interfaz ExportStrategy para exportar horarios en formato PDF.
 */
@RequiredArgsConstructor
public class PdfExportStrategy implements ExportStrategy {

    @Override
    public byte[] export(List<schedule> schedules, String title) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // Configurar fuentes
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

        // Agregar título
        document.add(new Paragraph(title, titleFont));
        document.add(Chunk.NEWLINE);

        // Crear tabla
        PdfPTable table = createScheduleTable(schedules, headerFont, cellFont);
        document.add(table);

        document.close();
        return outputStream.toByteArray();
    }

    private PdfPTable createScheduleTable(List<schedule> schedules, Font headerFont, Font cellFont) throws DocumentException {
        String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] dayKeys = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        List<String> times = generateTimes(schedules);

        PdfPTable table = new PdfPTable(dayNames.length + 1);
        table.setWidthPercentage(100);

        // Configurar anchos de columna
        float[] columnWidths = new float[dayNames.length + 1];
        columnWidths[0] = 1.5f; // Tiempo
        for (int i = 1; i < columnWidths.length; i++) {
            columnWidths[i] = 2f;
        }
        table.setWidths(columnWidths);

        BaseColor headerBg = new BaseColor(60, 120, 180);

        // Header: Tiempo + días
        addTableHeader(table, dayNames, headerFont, headerBg);

        // Contenido de la tabla
        addTableContent(table, schedules, times, dayKeys, cellFont);

        return table;
    }

    private void addTableHeader(PdfPTable table, String[] days, Font headerFont, BaseColor headerBg) {
        PdfPCell timeHeader = new PdfPCell(new Phrase("Tiempo", headerFont));
        timeHeader.setBackgroundColor(headerBg);
        timeHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(timeHeader);

        for (String day : days) {
            PdfPCell dayHeader = new PdfPCell(new Phrase(day, headerFont));
            dayHeader.setBackgroundColor(headerBg);
            dayHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(dayHeader);
        }
    }

    private void addTableContent(PdfPTable table, List<schedule> schedules, List<String> times, String[] days, Font cellFont) {
        for (String time : times) {
            // Celda de tiempo
            PdfPCell timeCell = new PdfPCell(new Phrase(time, cellFont));
            timeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(timeCell);

            // Celdas de contenido por día
            for (String day : days) {
                schedule s = getScheduleForTimeAndDay(schedules, time, day);
                String content = getScheduleContent(s, time);
                PdfPCell contentCell = new PdfPCell(new Phrase(content, cellFont));
                contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                // Colores especiales para descansos
                if (time.equals("9:00 AM - 9:30 AM") || time.equals("12:00 PM - 1:00 PM")) {
                    contentCell.setBackgroundColor(new BaseColor(255, 255, 204));
                }

                table.addCell(contentCell);
            }
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