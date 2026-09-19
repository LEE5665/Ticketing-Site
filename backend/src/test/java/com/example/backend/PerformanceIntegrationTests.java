package com.example.backend;

import com.example.backend.performance.entity.Performance;
import com.example.backend.performance.entity.PerformanceSchedule;
import com.example.backend.performance.repository.PerformanceRepository;
import com.example.backend.performance.repository.PerformanceScheduleRepository;
import com.example.backend.seat.entity.Seat;
import com.example.backend.seat.entity.SeatStatus;
import com.example.backend.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired PerformanceRepository performanceRepository;
    @Autowired PerformanceScheduleRepository scheduleRepository;
    @Autowired SeatRepository seatRepository;

    @BeforeEach
    void setup() {
        seatRepository.deleteAll();
        scheduleRepository.deleteAll();
        performanceRepository.deleteAll();
    }

    @Test
    void queryPerformancesSchedulesAndSeats() throws Exception {
        Performance performance = performanceRepository.save(new Performance(
                "test-concert", "테스트 콘서트", "TEST CONCERT", "콘서트",
                "체육관", "2026.11.01", 88000, "blue", "태그", "★"
        ));
        PerformanceSchedule schedule = scheduleRepository.save(
                new PerformanceSchedule(performance, LocalDateTime.of(2026, 11, 1, 19, 0))
        );
        seatRepository.save(new Seat(schedule, "A1", SeatStatus.AVAILABLE));
        seatRepository.save(new Seat(schedule, "A2", SeatStatus.RESERVED));

        // 1. 공연 목록 조회
        mvc.perform(get("/api/performances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("test-concert"))
                .andExpect(jsonPath("$[0].title").value("테스트 콘서트"))
                .andExpect(jsonPath("$[0].schedules[0].time").value("19:00"));

        // 2. 공연 단건 조회
        mvc.perform(get("/api/performances/test-concert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-concert"))
                .andExpect(jsonPath("$.schedules[0].id").value(schedule.getId()));

        // 3. 좌석 목록 조회
        mvc.perform(get("/api/schedules/" + schedule.getId() + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].seatNumber").value("A2"))
                .andExpect(jsonPath("$[1].available").value(false));
    }
}
