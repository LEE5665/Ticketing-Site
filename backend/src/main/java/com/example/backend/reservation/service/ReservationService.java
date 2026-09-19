package com.example.backend.reservation.service;

import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.performance.entity.PerformanceSchedule;
import com.example.backend.performance.repository.PerformanceScheduleRepository;
import com.example.backend.reservation.dto.MyReservationResponse;
import com.example.backend.reservation.dto.ReservationResponse;
import com.example.backend.reservation.entity.Reservation;
import com.example.backend.reservation.repository.ReservationRepository;
import com.example.backend.seat.entity.Seat;
import com.example.backend.seat.entity.SeatStatus;
import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final MemberRepository memberRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 예매 생성 (가예약)
     */
    @Transactional
    public ReservationResponse createSimpleReservation(String memberEmail, Long scheduleId, List<String> seatNumbers) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차 정보를 찾을 수 없습니다."));

        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("선택된 좌석이 없습니다.");
        }

        List<Seat> seats = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            Seat seat = seatRepository.findByScheduleIdAndSeatNumber(scheduleId, seatNumber)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + seatNumber));

            // 좌석 예매 가능 여부 확인
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("이미 예매된 좌석입니다: " + seatNumber);
            }
            seats.add(seat);
        }

        int pricePerSeat = schedule.getPerformance().getPrice();
        int totalAmount = pricePerSeat * seats.size();

        String orderId = "ORDER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String orderName = schedule.getPerformance().getTitle() + " " + seats.size() + "매 (" + String.join(", ", seatNumbers) + ")";

        Reservation reservation = new Reservation(member, schedule, orderId, orderName, totalAmount);
        for (Seat seat : seats) {
            reservation.addReservationSeat(seat);
        }

        reservationRepository.save(reservation);
        log.info("[예매 생성 완료] orderId={}, member={}, seats={}, totalAmount={}",
                orderId, member.getEmail(), seatNumbers, totalAmount);

        return ReservationResponse.from(reservation);
    }

    /**
     * 내 예매 내역 조회
     */
    @Transactional(readOnly = true)
    public List<MyReservationResponse> getMyReservations(String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        log.info("[내 예매 조회 시작] memberId={}", member.getId());

        // 1번 쿼리: reservations 조회
        List<Reservation> reservations = reservationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());

        // DTO 변환 및 반환
        return reservations.stream()
                .map(MyReservationResponse::from)
                .toList();
    }
}
