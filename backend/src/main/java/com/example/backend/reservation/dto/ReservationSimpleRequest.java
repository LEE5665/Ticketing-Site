package com.example.backend.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReservationSimpleRequest(
        @NotEmpty(message = "최소 1개 이상의 좌석을 선택해 주세요.")
        List<String> seatNumbers
) {}
