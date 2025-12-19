package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Estrategia de exportación a Excel para horarios académicos.
 * Implementa el patrón Strategy para exportar horarios en formato XLSX.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de exportar a Excel
 * - LSP: Intercambiable con otras estrategias de exportación
 * - DIP: Depende de la interfaz ExportStrategy
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class ExcelExportStrategy implements ExportStrategy {

    /**
     * Exporta la lista de horarios a un archivo Excel XLSX.
     * Crea un libro de trabajo con una hoja que contiene el horario formateado.
     *
     * @param schedules Lista de horarios a exportar
     * @param title Título del documento Excel
     * @return Array de bytes con el contenido del archivo Excel
     * @throws Exception si ocurre un error durante la exportación
     */
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

    /**
     * Crea el estilo para las celdas de encabezado.
     * Aplica fuente en negrita para destacar los encabezados.
     *
     * @param workbook Libro de trabajo de Excel
     * @return Estilo configurado para encabezados
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    /**
     * Agrega el título al documento Excel.
     * Coloca el título en la primera fila y lo fusiona en múltiples columnas.
     *
     * @param sheet Hoja de Excel donde agregar el título
     * @param title Texto del título
     * @param headerStyle Estilo a aplicar al título
     */
    private void addTitle(Sheet sheet, String title, CellStyle headerStyle) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
    }

    /**
     * Crea la tabla principal con los horarios.
     * Genera una tabla con tiempos en filas y días en columnas.
     *
     * @param sheet Hoja de Excel donde crear la tabla
     * @param schedules Lista de horarios a incluir
     * @param headerStyle Estilo para los encabezados de columna
     */
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

    /**
     * Ajusta automáticamente el ancho de las columnas para mejor legibilidad.
     *
     * @param sheet Hoja de Excel cuyas columnas ajustar
     */
    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i <= 6; i++) { // 0=Tiempo + 5 días + 1 extra
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Genera la lista de intervalos de tiempo para el horario.
     * Incluye tiempos fijos para descansos y clases.
     *
     * @param schedules Lista de horarios (no utilizada en esta implementación simplificada)
     * @return Lista de intervalos de tiempo formateados
     */
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

    /**
     * Busca un horario específico para un tiempo y día determinados.
     *
     * @param schedules Lista de horarios a buscar
     * @param time Intervalo de tiempo a buscar
     * @param day Día de la semana
     * @return Horario encontrado o null si no existe
     */
    private schedule getScheduleForTimeAndDay(List<schedule> schedules, String time, String day) {
        // Lógica simplificada para encontrar horario
        return schedules.stream()
            .filter(s -> s.getDay().equals(day) &&
                    time.contains(s.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a"))))
            .findFirst()
            .orElse(null);
    }

    /**
     * Genera el contenido de celda para un horario específico.
     * Maneja casos especiales como descansos y almuerzos.
     *
     * @param s Horario a procesar (puede ser null)
     * @param time Intervalo de tiempo para determinar el contenido
     * @return Contenido formateado para la celda
     */
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