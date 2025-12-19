package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

/**
 * Enumeración que representa los días laborables de la semana en el sistema SGH.
 * Solo incluye días de lunes a viernes, excluyendo fines de semana,
 * ya que el sistema está orientado a horarios académicos de días hábiles.
 *
 * Esta enumeración es utilizada principalmente para definir la disponibilidad
 * de profesores y la programación de clases.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Schema(description = "Días laborables de la semana (Lunes a Viernes)")
public enum Days {

    /**
     * Día lunes - Primer día laborable de la semana.
     */
    @Schema(description = "Lunes - Primer día laborable") Lunes,

    /**
     * Día martes - Segundo día laborable de la semana.
     */
    @Schema(description = "Martes - Segundo día laborable") Martes,

    /**
     * Día miércoles - Tercer día laborable de la semana.
     */
    @Schema(description = "Miércoles - Tercer día laborable") Miércoles,

    /**
     * Día jueves - Cuarto día laborable de la semana.
     */
    @Schema(description = "Jueves - Cuarto día laborable") Jueves,

    /**
     * Día viernes - Último día laborable de la semana.
     */
    @Schema(description = "Viernes - Último día laborable") Viernes;

    /**
     * Obtiene una lista ordenada de todos los días laborables.
     *
     * @return lista inmutable con todos los días
     */
    public static List<Days> getAllWorkDays() {
        return Arrays.asList(values());
    }

    /**
     * Verifica si el día es un día laborable válido.
     *
     * @param day día a verificar
     * @return true si es un día laborable
     */
    public static boolean isValidWorkDay(Days day) {
        return day != null && Arrays.asList(values()).contains(day);
    }

    /**
     * Convierte un DayOfWeek de Java a Days del sistema.
     * Solo funciona para días laborables (lunes-viernes).
     *
     * @param dayOfWeek día de la semana de Java
     * @return Days correspondiente o null si es fin de semana
     */
    public static Days fromDayOfWeek(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return Lunes;
            case TUESDAY: return Martes;
            case WEDNESDAY: return Miércoles;
            case THURSDAY: return Jueves;
            case FRIDAY: return Viernes;
            default: return null; // Sábado y Domingo no son días laborables
        }
    }

    /**
     * Obtiene el número ordinal del día (1=Lunes, 5=Viernes).
     *
     * @return número del día en la semana laboral
     */
    public int getDayNumber() {
        switch (this) {
            case Lunes: return 1;
            case Martes: return 2;
            case Miércoles: return 3;
            case Jueves: return 4;
            case Viernes: return 5;
            default: return 0;
        }
    }

    /**
     * Obtiene el nombre completo del día en español.
     *
     * @return nombre del día
     */
    public String getDisplayName() {
        switch (this) {
            case Lunes: return "Lunes";
            case Martes: return "Martes";
            case Miércoles: return "Miércoles";
            case Jueves: return "Jueves";
            case Viernes: return "Viernes";
            default: return "";
        }
    }

    /**
     * Verifica si el día es el primer día laborable (lunes).
     *
     * @return true si es lunes
     */
    public boolean isFirstWorkDay() {
        return this == Lunes;
    }

    /**
     * Verifica si el día es el último día laborable (viernes).
     *
     * @return true si es viernes
     */
    public boolean isLastWorkDay() {
        return this == Viernes;
    }
}