package com.example.backend.reservation.dto;

import com.example.backend.reservation.entity.Reservation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record MyReservationResponse(
        Long reservationId,
        String orderId,
        String status,
        int totalAmount,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String performanceTitle,
        String category,
        String venue,
        String color,
        String scheduleDate,
        String scheduleTime,
        List<String> seatNumbers
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // 연관 엔티티 조회 및 DTO 변환
    public static MyReservationResponse from(Reservation r) {
        var schedule = r.getSchedule();
        var performance = schedule.getPerformance();
        var seats = r.getReservationSeats().stream()
                .map(rs -> rs.getSeat().getSeatNumber())
                .sorted()
                .toList();

        return new MyReservationResponse(
                r.getId(),
                r.getOrderId(),
                r.getStatus().name(),
                r.getTotalAmount(),
                r.getCreatedAt(),
                r.getPaidAt(),
                performance.getTitle(),
                performance.getCategory(),
                performance.getVenue(),
                performance.getColor(),
                schedule.getStartTime().format(DATE_FMT),
                schedule.getStartTime().format(TIME_FMT),
                seats
        );
    }
}
