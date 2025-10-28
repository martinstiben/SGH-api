package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        List<String> times = generateTimes(schedules);

        // Header
        Row headerRow = sheet.createRow(2);
        headerRow.createCell(0).setCellValue("Tiempo");
        for (int i = 0; i < days.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(days[i]);
            cell.setCellStyle(headerStyle);
        }

        // Contenido
        int rowIdx = 3;
        for (String time : times) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(time);

            for (int i = 0; i < days.length; i++) {
                String day = days[i];
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