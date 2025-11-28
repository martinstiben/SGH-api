package com.horarios.SGH.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTableDTO {
    private String courseName;
    private String courseId;
    private List<String> days; // ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes"]
    private List<TimeSlotRow> timeSlots; // Filas de franjas horarias
    private int totalClasses;
    private Map<String, Integer> classesPerDay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotRow {
        private String timeRange; // "6:00-7:00", "7:00-8:00", etc.
        private Map<String, ScheduleCell> cells; // day -> cell content

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScheduleCell {
            private boolean hasClass;
            private String teacherName;
            private String subjectName;
            private String scheduleId;
            private String startTime;
            private String endTime;
        }
    }
}