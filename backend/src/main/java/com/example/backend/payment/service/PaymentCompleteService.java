package com.example.backend.payment.service;

import com.example.backend.payment.dto.PaymentConfirmResponse;
import com.example.backend.reservation.entity.Reservation;
import com.example.backend.reservation.entity.ReservationSeat;
import com.example.backend.reservation.entity.ReservationStatus;
import com.example.backend.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompleteService {

    private final ReservationRepository reservationRepository;

    /**
     * 토스 결제 승인 후 DB에 좌석 및 예약 상태를 최종 확정하는 트랜잭션
     * 외부 통신 완료 후 격리된 트랜잭션으로 상태를 커밋합니다.
     */
    @Transactional
    public PaymentConfirmResponse completeReservationAndSeats(String orderId, String paymentKey, int amount) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문 번호의 예약을 찾을 수 없습니다: " + orderId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.warn("이미 결제 완료된 예약입니다: orderId={}", orderId);
            return toResponse(reservation);
        }

        // [금액 위변조 방지 검증] DB의 가예약 금액과 토스 승인 결제 금액 일치 여부 확인
        if (reservation.getTotalAmount() != amount) {
            log.error("결제 금액 불일치 감지! DB 금액={}, 결제 금액={}", reservation.getTotalAmount(), amount);
            throw new IllegalStateException("결제 금액이 위변조되었거나 일치하지 않습니다.");
        }

        // 1. 예약 상태를 CONFIRMED로 변경
        reservation.confirm(paymentKey);

        // 2. 해당 예약에 묶인 좌석들을 RESERVED로 확정
        for (ReservationSeat rs : reservation.getReservationSeats()) {
            rs.getSeat().reserve();
        }

        log.info("[결제 완료 및 좌석 확정] orderId={}, reservationId={}, amount={}",
                orderId, reservation.getId(), amount);

        return toResponse(reservation);
    }

    private PaymentConfirmResponse toResponse(Reservation reservation) {
        List<String> seatNumbers = reservation.getReservationSeats().stream()
                .map(rs -> rs.getSeat().getSeatNumber())
                .sorted()
                .toList();

        return new PaymentConfirmResponse(
                true,
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getOrderName(),
                reservation.getTotalAmount(),
                reservation.getStatus().name(),
                reservation.getPaidAt(),
                seatNumbers
        );
    }
}
