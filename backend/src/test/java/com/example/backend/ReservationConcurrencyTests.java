package com.example.backend;

import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.performance.entity.Performance;
import com.example.backend.performance.entity.PerformanceSchedule;
import com.example.backend.performance.repository.PerformanceRepository;
import com.example.backend.performance.repository.PerformanceScheduleRepository;
import com.example.backend.reservation.repository.ReservationRepository;
import com.example.backend.reservation.repository.ReservationSeatRepository;
import com.example.backend.reservation.service.ReservationService;
import com.example.backend.seat.entity.Seat;
import com.example.backend.seat.entity.SeatStatus;
import com.example.backend.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyTests {

    @Autowired private ReservationService reservationService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private PerformanceScheduleRepository scheduleRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationSeatRepository reservationSeatRepository;

    private PerformanceSchedule schedule;
    private final List<Member> members = new ArrayList<>();

    @BeforeEach
    void setup() {
        reservationSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        scheduleRepository.deleteAll();
        performanceRepository.deleteAll();
        memberRepository.deleteAll();
        members.clear();

        // 100명의 테스트 사용자 생성
        for (int i = 1; i <= 100; i++) {
            Member member = memberRepository.save(new Member("유저" + i, "user" + i + "@test.com", "pass"));
            members.add(member);
        }

        Performance performance = performanceRepository.save(new Performance(
                "iu-concert", "아이유 콘서트", "IU CONCERT", "콘서트",
                "체조경기장", "2026.12.01", 130000, "purple", "힐링", "✦"
        ));

        schedule = scheduleRepository.save(
                new PerformanceSchedule(performance, LocalDateTime.of(2026, 12, 1, 19, 0))
        );

        // 경쟁할 좌석 2개 생성
        seatRepository.save(new Seat(schedule, "A1", SeatStatus.AVAILABLE));
        seatRepository.save(new Seat(schedule, "A2", SeatStatus.AVAILABLE));
    }

    @Test
    @DisplayName("동시에 100명이 동일한 좌석(A1, A2)을 예매 요청할 때, 비관적 락으로 인해 오직 1명만 성공해야 한다")
    void concurrencyReservationWithPessimisticLock() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        List<String> targetSeats = List.of("A1", "A2");

        // 100개의 동시 요청 실행
        for (int i = 0; i < numberOfThreads; i++) {
            final String email = members.get(i).getEmail();
            executorService.submit(() -> {
                try {
                    reservationService.createSimpleReservation(email, schedule.getId(), targetSeats);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 검증: 오직 1명만 선점 성공하고, 99명은 실패해야 함!
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // 생성된 예약 건수 검증
        assertThat(reservationRepository.count()).isEqualTo(1);

        // 좌석 상태가 HOLD로 변경되었는지 확인
        Seat a1 = seatRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "A1").orElseThrow();
        Seat a2 = seatRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "A2").orElseThrow();
        assertThat(a1.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(a2.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(a1.getHoldExpiresAt()).isNotNull();
        assertThat(a2.getHoldExpiresAt()).isNotNull();
    }
}
