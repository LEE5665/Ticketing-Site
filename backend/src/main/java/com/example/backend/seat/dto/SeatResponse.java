package com.example.backend.seat.dto;

import com.example.backend.seat.entity.Seat;

import java.time.LocalDateTime;

public record SeatResponse(
        Long id,
        String seatNumber,
        String status,
        boolean available
) {
    public static SeatResponse from(Seat seat, LocalDateTime now) {
        return new SeatResponse(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getStatus().name(),
                seat.isAvailable(now)
        );
    }
}
