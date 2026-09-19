package com.example.backend.global.init;

import com.example.backend.performance.entity.Performance;
import com.example.backend.performance.entity.PerformanceSchedule;
import com.example.backend.performance.repository.PerformanceRepository;
import com.example.backend.performance.repository.PerformanceScheduleRepository;
import com.example.backend.seat.entity.Seat;
import com.example.backend.seat.entity.SeatStatus;
import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (performanceRepository.count() > 0) {
            log.info("공연 및 좌석 데이터가 이미 존재하므로 초기화를 건너뜁니다.");
            return;
        }

        log.info("초기 공연 및 좌석 데이터를 생성합니다...");

        // 1. 블루 아워 라이브 (콘서트)
        Performance blueHour = performanceRepository.save(new Performance(
                "blue-hour", "블루 아워 라이브", "BLUE HOUR", "콘서트",
                "서울 올림픽홀", "2026.11.07 – 11.08", 99000,
                "blue", "가을의 밤을 채울 목소리", "◒"
        ));
        createSchedulesAndSeats(blueHour, List.of("2026-11-07", "2026-11-08"), true);

        // 2. 미드나잇 익스프레스 (뮤지컬)
        Performance midnight = performanceRepository.save(new Performance(
                "midnight", "미드나잇 익스프레스", "MIDNIGHT\nEXPRESS", "뮤지컬",
                "서울 아트씨어터", "2026.11.14 – 11.15", 85000,
                "purple", "마지막 기차에서 시작된 이야기", "✦"
        ));
        createSchedulesAndSeats(midnight, List.of("2026-11-14", "2026-11-15"), true);

        // 3. 그린 데이즈 페스티벌 (페스티벌)
        Performance greenDays = performanceRepository.save(new Performance(
                "green-days", "그린 데이즈 페스티벌", "GREEN\nDAYS", "페스티벌",
                "한강 잔디공원", "2026.11.21 – 11.22", 110000,
                "green", "음악과 함께하는 느긋한 하루", "✳"
        ));
        createSchedulesAndSeats(greenDays, List.of("2026-11-21", "2026-11-22"), false);

        // 4. 빛의 방 (전시)
        Performance roomOfLight = performanceRepository.save(new Performance(
                "room-of-light", "빛의 방", "ROOM\nOF LIGHT", "전시",
                "성수 아트스페이스", "2026.11.28 – 11.29", 22000,
                "orange", "빛과 공간 사이, 새로운 감각", "◐"
        ));
        createSchedulesAndSeats(roomOfLight, List.of("2026-11-28", "2026-11-29"), false);

        log.info("초기 공연, 회차, 좌석 데이터 생성이 완료되었습니다!");
    }

    private void createSchedulesAndSeats(Performance performance, List<String> dates, boolean assignedSeats) {
        List<LocalTime> times = List.of(LocalTime.of(14, 0), LocalTime.of(19, 0));
        Set<String> sampleReservedSeats = Set.of("A3", "A4", "B6", "C2");

        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            for (LocalTime time : times) {
                PerformanceSchedule schedule = scheduleRepository.save(
                        new PerformanceSchedule(performance, LocalDateTime.of(date, time))
                );

                List<Seat> seats = new ArrayList<>();
                if (assignedSeats) {
                    // 지정 좌석: A1 ~ D8 (총 32석)
                    for (String row : List.of("A", "B", "C", "D")) {
                        for (int i = 1; i <= 8; i++) {
                            String seatNum = row + i;
                            SeatStatus status = sampleReservedSeats.contains(seatNum)
                                    ? SeatStatus.RESERVED
                                    : SeatStatus.AVAILABLE;
                            seats.add(new Seat(schedule, seatNum, status));
                        }
                    }
                } else {
                    // 비지정석 (입장권): G1 ~ G50 (총 50매)
                    for (int i = 1; i <= 50; i++) {
                        seats.add(new Seat(schedule, "G" + i, SeatStatus.AVAILABLE));
                    }
                }
                seatRepository.saveAll(seats);
            }
        }
    }
}
