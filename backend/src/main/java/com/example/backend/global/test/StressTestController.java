package com.example.backend.global.test;

import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.reservation.dto.ReservationResponse;
import com.example.backend.reservation.repository.ReservationRepository;
import com.example.backend.reservation.repository.ReservationSeatRepository;
import com.example.backend.reservation.service.ReservationService;
import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class StressTestController {

    private final ReservationService reservationService;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    public record StressReservationRequest(
            String email,
            Long scheduleId,
            List<String> seatNumbers
    ) {}

    /**
     * 부하 테스트용 예매 API (인증/세션 없이 순수 동시성 락 성능 측정)
     */
    @PostMapping("/reservations")
    public ResponseEntity<?> stressReservation(@RequestBody StressReservationRequest request) {
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email()
                : "user@test.com";

        // 테스트용 회원이 없으면 자동 생성하여 FK 에러 방지
        memberRepository.findByEmail(email).orElseGet(() ->
                memberRepository.save(new Member("스트레스유저", email, "password"))
        );

        try {
            ReservationResponse response = reservationService.createSimpleReservation(
                    email,
                    request.scheduleId(),
                    request.seatNumbers()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            // 이미 선점된 좌석이거나 동시성 경합에서 밀린 경우 409 Conflict 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 테스트 전/후 좌석 상태 및 예약 데이터 초기화 (반복 테스트 편의용)
     */
    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<String> resetData() {
        reservationSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.releaseExpiredSeats(LocalDateTime.now().plusDays(100)); // 모든 HOLD를 AVAILABLE로 복구
        log.info("[부하 테스트 데이터 초기화 완료]");
        return ResponseEntity.ok("테스트 데이터가 초기화되었습니다.");
    }
}
