package com.example.backend.seat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {
    private final RedissonClient redisson;

    public <T> T withLocks(Long scheduleId, List<String> seatNumbers, Supplier<T> action) {
        List<RLock> acquired = new ArrayList<>();
        try {
            for (String seatNumber : seatNumbers.stream().distinct().sorted().toList()) {
                RLock lock = redisson.getLock("seat:lock:" + scheduleId + ":" + seatNumber);
                // 대기/재시도 없이 획득. leaseTime을 지정하지 않아 watchdog으로 연장한다.
                if (!lock.tryLock()) {
                    throw new SeatConflictException("다른 사용자가 해당 좌석을 처리 중입니다.");
                }
                acquired.add(lock);
            }
            return action.get();
        } finally {
            for (int i = acquired.size() - 1; i >= 0; i--) {
                try {
                    // Redisson이 소유자를 원자적으로 확인하므로 다른 요청의 락은 해제되지 않는다.
                    acquired.get(i).unlock();
                } catch (RuntimeException error) {
                    // 커밋된 예약을 실패로 응답하지 않고 나머지 락도 해제한다.
                    log.error("좌석 락 해제 실패: {}", acquired.get(i).getName(), error);
                }
            }
        }
    }
}
