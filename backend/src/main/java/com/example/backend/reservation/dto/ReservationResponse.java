package com.example.backend.reservation.dto;

import com.example.backend.reservation.entity.Reservation;

public record ReservationResponse(
        Long reservationId,
        String orderId,
        String orderName,
        int amount,
        String customerName,
        String customerEmail
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getOrderName(),
                reservation.getTotalAmount(),
                reservation.getMember().getName(),
                reservation.getMember().getEmail()
        );
    }
}
