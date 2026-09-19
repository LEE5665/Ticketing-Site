package com.example.backend.performance.dto;

import com.example.backend.performance.entity.Performance;
import com.example.backend.performance.entity.PerformanceSchedule;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record PerformanceResponse(
        Long id,
        String slug,
        String title,
        String subtitle,
        String category,
        String venue,
        String date,
        int price,
        String color,
        String tag,
        String symbol,
        List<ScheduleDto> schedules
) {
    public record ScheduleDto(
            Long id,
            String date,
            String time
    ) {
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

        public static ScheduleDto from(PerformanceSchedule schedule) {
            return new ScheduleDto(
                    schedule.getId(),
                    schedule.getStartTime().format(DATE_FMT),
                    schedule.getStartTime().format(TIME_FMT)
            );
        }
    }

    public static PerformanceResponse from(Performance performance, List<PerformanceSchedule> schedules) {
        return new PerformanceResponse(
                performance.getId(),
                performance.getSlug(),
                performance.getTitle(),
                performance.getSubtitle(),
                performance.getCategory(),
                performance.getVenue(),
                performance.getDateText(),
                performance.getPrice(),
                performance.getColor(),
                performance.getTag(),
                performance.getSymbol(),
                schedules.stream().map(ScheduleDto::from).toList()
        );
    }
}
