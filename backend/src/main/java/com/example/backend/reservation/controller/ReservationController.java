package com.example.backend.reservation.controller;

import com.example.backend.reservation.dto.MyReservationResponse;
import com.example.backend.reservation.dto.ReservationResponse;
import com.example.backend.reservation.dto.ReservationSimpleRequest;
import com.example.backend.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예매 신청 (가예약 생성)
     */
    @PostMapping("/api/schedules/{scheduleId}/reservations/simple")
    public ResponseEntity<ReservationResponse> createSimpleReservation(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ReservationSimpleRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        ReservationResponse response = reservationService.createSimpleReservation(
                email,
                scheduleId,
                request.seatNumbers()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 예매 내역 조회
     */
    @GetMapping("/api/reservations/my")
    public ResponseEntity<List<MyReservationResponse>> getMyReservations(
            Authentication authentication
    ) {
        String email = authentication.getName();
        List<MyReservationResponse> myReservations = reservationService.getMyReservations(email);
        return ResponseEntity.ok(myReservations);
    }

    /**
     * 예매 취소 (결제 전 이탈 또는 취소 시 좌석 선점 해제)
     */
    @PostMapping("/api/reservations/{orderId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable String orderId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        reservationService.cancelReservation(email, orderId);
        return ResponseEntity.noContent().build();
    }
}
