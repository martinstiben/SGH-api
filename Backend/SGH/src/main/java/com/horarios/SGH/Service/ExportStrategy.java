package com.horarios.SGH.Service;

import com.horarios.SGH.Model.schedule;
import java.util.List;

/**
 * Interfaz Strategy para diferentes formatos de exportación de horarios.
 * Define el contrato para estrategias de exportación intercambiables.
 *
 * Principios SOLID aplicados:
 * - ISP: Interfaz específica y pequeña
 * - DIP: Las clases dependen de esta abstracción
 *
 * Implementaciones disponibles:
 * - ExcelExportStrategy: Exporta a formato Excel (.xlsx)
 * - PdfExportStrategy: Exporta a formato PDF
 * - ImageExportStrategy: Exporta a formato imagen PNG
 *
 * @author Sistema SGH
 * @version 1.0
 */
public interface ExportStrategy {

    /**
     * Exporta una lista de horarios a un formato específico.
     * Cada implementación concreta determina el formato de salida.
     *
     * @param schedules Lista de horarios académicos a exportar. No debe ser null.
     * @param title Título del documento a generar. Se utiliza para encabezados o metadatos.
     * @return Array de bytes con el contenido del archivo exportado en el formato correspondiente
     * @throws Exception si ocurre un error durante el proceso de exportación
     *         (ej. problemas de I/O, formato inválido, etc.)
     */
    byte[] export(List<schedule> schedules, String title) throws Exception;
}