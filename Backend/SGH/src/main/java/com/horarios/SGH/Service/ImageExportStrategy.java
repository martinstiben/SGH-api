package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Estrategia de exportación a imagen PNG.
 * Implementa la interfaz ExportStrategy para exportar horarios en formato de imagen.
 */
public class ImageExportStrategy implements ExportStrategy {

    @Override
    public byte[] export(List<schedule> schedules, String title) throws Exception {
        List<String> times = generateTimes(schedules);
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};

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
        drawTable(g, schedules, times, days, padding, rowHeight);

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

    private void drawTable(Graphics2D g, List<schedule> schedules, List<String> times, String[] days, int padding, int rowHeight) {
        int y = padding + rowHeight;

        // Headers
        g.setFont(new Font("Arial", Font.BOLD, 12));
        int[] xPositions = {20, 150, 350, 550, 650, 750, 850, 950};
        g.drawString("Tiempo", xPositions[0], y);
        for (int i = 0; i < days.length; i++) {
            g.drawString(days[i], xPositions[i + 1], y);
        }

        y += rowHeight;
        g.setFont(new Font("Arial", Font.PLAIN, 11));

        // Contenido
        for (String time : times) {
            g.drawString(time, xPositions[0], y);

            for (int i = 0; i < days.length; i++) {
                String day = days[i];
                schedule s = getScheduleForTimeAndDay(schedules, time, day);
                String content = getScheduleContent(s, time);
                g.drawString(content, xPositions[i + 1], y);
            }
            y += rowHeight;
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