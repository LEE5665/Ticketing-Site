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
import com.example.backend.reservation.entity.ReservationStatus;
import com.example.backend.seat.entity.Seat;
import com.example.backend.seat.entity.SeatStatus;
import com.example.backend.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;
    @Autowired PerformanceRepository performanceRepository;
    @Autowired PerformanceScheduleRepository scheduleRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired ReservationSeatRepository reservationSeatRepository;
    @Autowired ReservationService reservationService;

    private Member member;
    private PerformanceSchedule schedule;

    @Test
    void cancellingExpiredReservationDoesNotReleaseNewOwnersHold() {
        var first = reservationService.createSimpleReservation(member.getEmail(), schedule.getId(), List.of("A1", "A2"));
        var original = reservationRepository.findByOrderId(first.orderId()).orElseThrow();
        Seat expired = seatRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "A1").orElseThrow();
        expired.hold(LocalDateTime.now().minusMinutes(1), original.getHoldToken());
        seatRepository.saveAndFlush(expired);

        Member other = memberRepository.save(new Member("다른 사용자", "other@test.com", "pass"));
        var second = reservationService.createSimpleReservation(other.getEmail(), schedule.getId(), List.of("A1"));
        var newOwner = reservationRepository.findByOrderId(second.orderId()).orElseThrow();
        Seat before = seatRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "A1").orElseThrow();
        assertThat(newOwner.getHoldToken()).isNotEqualTo(original.getHoldToken());

        reservationService.cancelReservation(member.getEmail(), first.orderId());
        reservationService.cancelReservation(member.getEmail(), first.orderId());

        Seat after = seatRepository.findById(before.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(after.getHoldToken()).isEqualTo(newOwner.getHoldToken());
        assertThat(after.getHoldExpiresAt()).isEqualTo(before.getHoldExpiresAt());
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
        assertThat(reservationRepository.findByOrderId(first.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservationRepository.findByOrderId(second.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(seatRepository.findByScheduleIdAndSeatNumber(schedule.getId(), "A2").orElseThrow().getStatus())
                .isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void cancellingOwnedHoldReleasesAllSeatsAndClearsTokens() {
        var response = reservationService.createSimpleReservation(member.getEmail(), schedule.getId(), List.of("A1", "A2"));
        String token = reservationRepository.findByOrderId(response.orderId()).orElseThrow().getHoldToken();
        assertThat(token).isNotBlank();
        assertThat(seatRepository.findByScheduleIdOrderBySeatNumberAsc(schedule.getId()))
                .allSatisfy(seat -> assertThat(seat.getHoldToken()).isEqualTo(token));

        assertThatThrownBy(() -> reservationService.cancelReservation("other@test.com", response.orderId()))
                .isInstanceOf(IllegalArgumentException.class);
        reservationService.cancelReservation(member.getEmail(), response.orderId());

        assertThat(reservationRepository.findByOrderId(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(seatRepository.findByScheduleIdOrderBySeatNumberAsc(schedule.getId()))
                .allSatisfy(seat -> {
                    assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
                    assertThat(seat.getHoldToken()).isNull();
                    assertThat(seat.getHoldExpiresAt()).isNull();
                });
    }

    @BeforeEach
    void setup() {
        reservationSeatRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        scheduleRepository.deleteAll();
        performanceRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member("홍길동", "user@test.com", "pass"));
        Performance performance = performanceRepository.save(new Performance(
                "iu-concert", "아이유 콘서트", "IU CONCERT", "콘서트",
                "체조경기장", "2026.12.01", 130000, "purple", "힐링", "✦"
        ));
        schedule = scheduleRepository.save(
                new PerformanceSchedule(performance, LocalDateTime.of(2026, 12, 1, 19, 0))
        );
        seatRepository.save(new Seat(schedule, "A1", SeatStatus.AVAILABLE));
        seatRepository.save(new Seat(schedule, "A2", SeatStatus.AVAILABLE));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void testSimpleReservationAndMyReservations() throws Exception {
        // 1. 가예약 요청
        String body = """
                {"seatNumbers":["A1","A2"]}
                """;

        var result = mvc.perform(post("/api/schedules/" + schedule.getId() + "/reservations/simple")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.amount").value(260000))
                .andExpect(jsonPath("$.customerEmail").value("user@test.com"))
                .andReturn();

        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(reservationSeatRepository.count()).isEqualTo(2);

        // 2. 내 예매 내역 조회
        mvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].performanceTitle").value("아이유 콘서트"))
                .andExpect(jsonPath("$[0].totalAmount").value(260000))
                .andExpect(jsonPath("$[0].seatNumbers[0]").value("A1"))
                .andExpect(jsonPath("$[0].seatNumbers[1]").value("A2"));
    }
}
