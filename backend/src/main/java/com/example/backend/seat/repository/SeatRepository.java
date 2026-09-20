package com.example.backend.seat.repository;

import com.example.backend.seat.entity.Seat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleIdOrderBySeatNumberAsc(Long scheduleId);
    Optional<Seat> findByScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);

    /**
     * 비관적 락(SELECT ... FOR UPDATE)으로 좌석들을 좌석번호 오름차순으로 조회 (데드락 방지)
     * 락 타임아웃 3초 설정
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT s FROM Seat s WHERE s.schedule.id = :scheduleId AND s.seatNumber IN :seatNumbers ORDER BY s.seatNumber ASC")
    List<Seat> findByScheduleIdAndSeatNumberInWithLock(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNumbers") List<String> seatNumbers
    );

    /**
     * 낙관적 락 방식으로 좌석들을 조회 (SELECT FOR UPDATE 없이 조회, Entity @Version으로 커밋 시 충돌 감지)
     */
    @Query("SELECT s FROM Seat s WHERE s.schedule.id = :scheduleId AND s.seatNumber IN :seatNumbers ORDER BY s.seatNumber ASC")
    List<Seat> findByScheduleIdAndSeatNumberIn(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNumbers") List<String> seatNumbers
    );

    /**
     * 5분 만료된 HOLD 좌석을 일괄 AVAILABLE로 복구하는 벌크 쿼리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.holdExpiresAt = null, s.holdToken = null, s.version = s.version + 1 WHERE s.status = 'HOLD' AND s.holdExpiresAt < :now")
    int releaseExpiredSeats(@Param("now") LocalDateTime now);

    /**
     * 부하 테스트용: 특정 회차의 모든 좌석을 AVAILABLE로 강제 초기화
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.holdExpiresAt = null, s.holdToken = null, s.version = s.version + 1 WHERE s.schedule.id = :scheduleId")
    int resetAllSeatsByScheduleId(@Param("scheduleId") Long scheduleId);

    // 소유권 확인과 해제를 하나의 UPDATE로 처리하고 기존 낙관적 락도 무효화한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.holdExpiresAt = null, s.holdToken = null, s.version = s.version + 1 WHERE s.id = :seatId AND s.status = 'HOLD' AND s.holdToken = :holdToken")
    int releaseHeldSeat(@Param("seatId") Long seatId, @Param("holdToken") String holdToken);
}
