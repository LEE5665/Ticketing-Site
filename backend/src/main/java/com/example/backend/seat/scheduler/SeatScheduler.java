package com.example.backend.seat.scheduler;

import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatScheduler {

    private final SeatRepository seatRepository;

    /**
     * 10초마다 5분 선점(HOLD) 유효시간이 만료된 좌석을 일괄 AVAILABLE 상태로 복구
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void cleanupExpiredSeatHolds() {
        LocalDateTime now = LocalDateTime.now();
        int releasedCount = seatRepository.releaseExpiredSeats(now);
        if (releasedCount > 0) {
            log.info("[좌석 만료 자동 회수] 만료된 선점 좌석 {}개를 AVAILABLE 상태로 자동 복구했습니다.", releasedCount);
        }
    }
}
