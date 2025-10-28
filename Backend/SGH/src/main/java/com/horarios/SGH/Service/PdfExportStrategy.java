package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        List<String> times = generateTimes(schedules);

        PdfPTable table = new PdfPTable(days.length + 1);
        table.setWidthPercentage(100);

        // Configurar anchos de columna
        float[] columnWidths = new float[days.length + 1];
        columnWidths[0] = 1.5f; // Tiempo
        for (int i = 1; i < columnWidths.length; i++) {
            columnWidths[i] = 2f;
        }
        table.setWidths(columnWidths);

        BaseColor headerBg = new BaseColor(60, 120, 180);

        // Header: Tiempo + días
        addTableHeader(table, days, headerFont, headerBg);

        // Contenido de la tabla
        addTableContent(table, schedules, times, days, cellFont);

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
        // Lógica simplificada para generar tiempos únicos
        return List.of(
            "9:00 AM - 9:30 AM",
            "9:30 AM - 10:30 AM",
            "10:30 AM - 11:30 AM",
            "11:30 AM - 12:00 PM",
            "12:00 PM - 1:00 PM",
            "1:00 PM - 2:00 PM",
            "2:00 PM - 3:00 PM",
            "3:00 PM - 4:00 PM",
            "4:00 PM - 5:00 PM"
        );
    }

    private schedule getScheduleForTimeAndDay(List<schedule> schedules, String time, String day) {
        // Lógica simplificada para encontrar horario
        return schedules.stream()
            .filter(s -> s.getDay().equals(day) &&
                    time.contains(s.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a"))))
            .findFirst()
            .orElse(null);
    }

    private String getScheduleContent(schedule s, String time) {
        if (time.equals("9:00 AM - 9:30 AM")) {
            return "Descanso";
        } else if (time.equals("12:00 PM - 1:00 PM")) {
            return "Almuerzo";
        } else if (s != null) {
            String docente = s.getTeacherId() != null ? s.getTeacherId().getTeacherName() : "";
            String materia = s.getSubjectId() != null ? s.getSubjectId().getSubjectName() : "";
            return docente + "/" + materia;
        }
        return "";
    }
}