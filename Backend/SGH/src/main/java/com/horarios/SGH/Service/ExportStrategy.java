package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import java.util.List;

/**
 * Interfaz para estrategias de exportación.
 * Aplica el patrón Strategy para diferentes formatos de exportación.
 */
public interface ExportStrategy {

    /**
     * Exporta los horarios a un formato específico.
     *
     * @param schedules Lista de horarios a exportar
     * @param title Título del documento
     * @return Array de bytes con el contenido exportado
     * @throws Exception si ocurre un error durante la exportación
     */
    byte[] export(List<schedule> schedules, String title) throws Exception;
}