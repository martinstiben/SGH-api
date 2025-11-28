package com.horarios.SGH.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVerificationReportDTO {
    private String courseName;
    private Integer courseId;
    private boolean complete;
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> successes;

    // Estadísticas del horario
    private ScheduleStatistics statistics;

    // Verificaciones específicas
    private ProtectedBlocksVerification protectedBlocks;
    private ContentVerification content;
    private ConflictsVerification conflicts;
    private FormatVerification format;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleStatistics {
        private int totalClasses;
        private int totalSlotsAvailable;
        private int filledSlots;
        private double coveragePercentage;
        private Map<String, Integer> classesPerDay;
        private Map<String, Integer> classesPerTimeSlot;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProtectedBlocksVerification {
        private boolean breakTimeRespected;
        private boolean lunchTimeRespected;
        private List<String> violations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentVerification {
        private boolean allSlotsFilled;
        private boolean noEmptyCells;
        private boolean noDuplicateCells;
        private boolean consistentData;
        private List<String> emptySlots;
        private List<String> duplicateSlots;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictsVerification {
        private boolean noTeacherConflicts;
        private boolean noTimeSlotConflicts;
        private boolean logicalDistribution;
        private List<String> teacherConflicts;
        private List<String> timeConflicts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormatVerification {
        private boolean correctStructure;
        private boolean readableFormat;
        private boolean professionalLayout;
        private List<String> formatIssues;
    }
}