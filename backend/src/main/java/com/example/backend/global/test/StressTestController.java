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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
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

        try {
            ReservationResponse response = reservationService.createSimpleReservation(
                    email,
                    request.scheduleId(),
                    request.seatNumbers()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException | ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            // 이미 선점된 좌석이거나 동시성(낙관적 락) 경합에서 밀린 경우 409 Conflict 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 선점되었거나 다른 사용자가 먼저 예매 중인 좌석입니다.");
        } catch (org.redisson.client.RedisException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("좌석 잠금 서버를 사용할 수 없습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 측정 중 회원 생성/조회가 좌석 락보다 먼저 DB에 접근하지 않도록 준비 단계로 분리한다.
    @PostMapping("/members")
    @Transactional
    public ResponseEntity<Void> prepareMembers(@RequestParam(defaultValue = "1000") int count) {
        if (count < 1 || count > 10000) {
            throw new IllegalArgumentException("회원 수는 1~10000 사이여야 합니다.");
        }
        for (int i = 1; i <= count; i++) {
            String email = "stress_user_" + i + "@test.com";
            if (memberRepository.findByEmail(email).isEmpty()) {
                memberRepository.save(new Member("스트레스유저", email, "password"));
            }
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 테스트 전/후 좌석 상태 및 예약 데이터 초기화 (반복 테스트 편의용)
     */
    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<String> resetData() {
        reservationSeatRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        seatRepository.resetAllSeatsByScheduleId(1L); // 1번 스케줄의 모든 좌석을 AVAILABLE로 복구
        log.info("[부하 테스트 데이터 초기화 완료]");
        return ResponseEntity.ok("테스트 데이터가 초기화되었습니다.");
    }
}
