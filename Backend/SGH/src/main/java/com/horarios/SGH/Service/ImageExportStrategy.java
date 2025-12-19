package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Estrategia de exportación a imagen PNG.
 * Implementa la interfaz ExportStrategy para exportar horarios en formato de imagen.
 */
public class ImageExportStrategy implements ExportStrategy {

    @Override
    public byte[] export(List<schedule> schedules, String title) throws Exception {
        List<String> times = generateTimes(schedules);
        String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] dayKeys = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};

        // Dimensiones de la imagen
        int width = 1400;
        int rowHeight = 25;
        int padding = 40;
        int height = padding + (times.size() + 2) * rowHeight;

        // Crear imagen
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Configurar fondo y colores
        setupGraphics(g, width, height);

        // Dibujar título
        drawTitle(g, title, padding);

        // Dibujar tabla
        drawTable(g, schedules, times, dayNames, dayKeys, padding, rowHeight);

        g.dispose();

        // Convertir a bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private void setupGraphics(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(30, 30, 30));
    }

    private void drawTitle(Graphics2D g, String title, int padding) {
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(title, 20, padding);
    }

    private void drawTable(Graphics2D g, List<schedule> schedules, List<String> times, String[] dayNames, String[] dayKeys, int padding, int rowHeight) {
        int y = padding + rowHeight;

        // Headers
        g.setFont(new Font("Arial", Font.BOLD, 12));
        int[] xPositions = {20, 150, 350, 550, 650, 750, 850, 950};
        g.drawString("Tiempo", xPositions[0], y);
        for (int i = 0; i < dayNames.length; i++) {
            g.drawString(dayNames[i], xPositions[i + 1], y);
        }

        y += rowHeight;
        g.setFont(new Font("Arial", Font.PLAIN, 11));

        // Contenido
        for (String time : times) {
            g.drawString(time, xPositions[0], y);

            for (int i = 0; i < dayKeys.length; i++) {
                String day = dayKeys[i];
                schedule s = getScheduleForTimeAndDay(schedules, time, day);
                String content = getScheduleContent(s, time);
                g.drawString(content, xPositions[i + 1], y);
            }
            y += rowHeight;
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