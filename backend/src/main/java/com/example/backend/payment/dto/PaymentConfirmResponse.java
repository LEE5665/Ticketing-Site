package com.example.backend.payment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentConfirmResponse(
        boolean success,
        Long reservationId,
        String orderId,
        String orderName,
        int amount,
        String status,
        LocalDateTime paidAt,
        List<String> seatNumbers
) {}
