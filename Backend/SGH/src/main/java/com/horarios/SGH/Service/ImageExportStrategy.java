package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Estrategia de exportación a imagen PNG para horarios académicos.
 * Implementa el patrón Strategy para exportar horarios en formato de imagen.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de exportar a imagen PNG
 * - LSP: Intercambiable con otras estrategias de exportación
 * - DIP: Depende de la interfaz ExportStrategy
 *
 * Características:
 * - Genera imagen PNG con tabla de horarios
 * - Diseño simple y legible
 * - Colores diferenciados para descansos y almuerzos
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class ImageExportStrategy implements ExportStrategy {

    /**
     * Exporta la lista de horarios a un archivo de imagen PNG.
     * Crea una imagen con una tabla que muestra el horario formateado.
     *
     * @param schedules Lista de horarios a exportar
     * @param title Título a mostrar en la parte superior de la imagen
     * @return Array de bytes con el contenido de la imagen PNG
     * @throws Exception si ocurre un error durante la generación de la imagen
     */
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

    /**
     * Configura el contexto gráfico para el dibujo de la imagen.
     * Establece colores de fondo y texto por defecto.
     *
     * @param g Contexto gráfico 2D
     * @param width Ancho de la imagen
     * @param height Alto de la imagen
     */
    private void setupGraphics(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(30, 30, 30));
    }

    /**
     * Dibuja el título en la parte superior de la imagen.
     *
     * @param g Contexto gráfico donde dibujar
     * @param title Texto del título
     * @param padding Espacio de relleno desde el borde superior
     */
    private void drawTitle(Graphics2D g, String title, int padding) {
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(title, 20, padding);
    }

    /**
     * Dibuja la tabla completa con horarios en la imagen.
     * Incluye encabezados de días y contenido de cada celda.
     *
     * @param g Contexto gráfico donde dibujar
     * @param schedules Lista de horarios a mostrar
     * @param times Lista de intervalos de tiempo
     * @param days Array de nombres de días
     * @param padding Espacio de relleno
     * @param rowHeight Alto de cada fila
     */
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

    /**
     * Genera la lista de intervalos de tiempo para el horario.
     * Incluye tiempos fijos para clases, descansos y almuerzo.
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