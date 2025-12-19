package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar el historial de generación de horarios.
 * 
 * NOTA: Este servicio actualmente no persiste el historial en base de datos
 * ya que las entidades correspondientes (IScheduleHistory, schedule_history)
 * no están implementadas en el proyecto actual.
 * 
 * Retorna páginas vacías hasta que se implemente la funcionalidad de historial.
 */
@Service
@RequiredArgsConstructor
public class ScheduleHistoryService {

    /**
     * Obtiene el historial de generación de horarios.
     * 
     * Actualmente retorna una página vacía ya que la funcionalidad
     * de historial persistente no está implementada.
     * 
     * @param page número de página (0-based)
     * @param size tamaño de página
     * @return página vacía de ScheduleHistoryDTO
     */
    public Page<ScheduleHistoryDTO> history(int page, int size) {
        // TODO: Implementar persistencia de historial cuando se creen las entidades correspondientes
        // Por ahora retornamos una página vacía
        
        List<ScheduleHistoryDTO> emptyContent = new ArrayList<>();
        var pageable = PageRequest.of(page, size);
        
        System.out.println("=== CONSULTA DE HISTORIAL DE HORARIOS ===");
        System.out.println("Página: " + page + ", Tamaño: " + size);
        System.out.println("NOTA: Funcionalidad de historial no implementada aún. Retornando página vacía.");
        
        return new PageImpl<>(emptyContent, pageable, 0);
    }

    /**
     * Método privado para convertir entidades a DTO.
     * Mantenido para compatibilidad futura cuando se implemente el historial.
     */
    private ScheduleHistoryDTO toDTO(Object entity) {
        // TODO: Implementar conversión cuando se tenga la entidad schedule_history
        ScheduleHistoryDTO dto = new ScheduleHistoryDTO();
        // Configurar campos por defecto o dejar vacío
        dto.setCoursesWithoutAvailability(new ArrayList<>());
        dto.setTotalCoursesWithoutAvailability(0);
        return dto;
    }
}