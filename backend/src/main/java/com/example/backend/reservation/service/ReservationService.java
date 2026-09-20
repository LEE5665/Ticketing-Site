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
import com.example.backend.seat.service.SeatLockService;
import com.example.backend.seat.service.SeatConflictException;
import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
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
    private final SeatLockService seatLockService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Redis 락 획득 후 DB 트랜잭션을 시작하고 커밋/롤백 완료 후 해제한다.
     */
    @Transactional(propagation = Propagation.NEVER)
    public ReservationResponse createSimpleReservation(String memberEmail, Long scheduleId, List<String> seatNumbers) {
        if (scheduleId == null || seatNumbers == null || seatNumbers.isEmpty()
                || seatNumbers.stream().anyMatch(seat -> seat == null || seat.isBlank())) {
            throw new IllegalArgumentException("회차와 좌석을 지정해야 합니다.");
        }
        return seatLockService.withLocks(scheduleId, seatNumbers, () ->
                new TransactionTemplate(transactionManager).execute(status ->
                        createReservationInTransaction(memberEmail, scheduleId, seatNumbers)));
    }

    private ReservationResponse createReservationInTransaction(String memberEmail, Long scheduleId, List<String> seatNumbers) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차 정보를 찾을 수 없습니다."));

        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("선택된 좌석이 없습니다.");
        }

        // 데드락(Deadlock) 방지를 위해 좌석 번호 오름차순 정렬 및 중복 제거
        List<String> sortedSeatNumbers = seatNumbers.stream().distinct().sorted().toList();

        // 1. 낙관적 락으로 좌석들을 일괄 조회 (SELECT ... FOR UPDATE 없음, Entity의 @Version으로 트랜잭션 커밋 시 충돌 감지)
        List<Seat> seats = seatRepository.findByScheduleIdAndSeatNumberIn(scheduleId, sortedSeatNumbers);

        if (seats.size() != sortedSeatNumbers.size()) {
            throw new IllegalArgumentException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        // 2. 각 좌석의 예매 가능 여부 확인 (AVAILABLE 또는 5분 만료된 HOLD인지 체크)
        for (Seat seat : seats) {
            if (!seat.isAvailable(now)) {
                throw new SeatConflictException("이미 선택되었거나 예매된 좌석입니다: " + seat.getSeatNumber());
            }
        }

        // 3. 5분간 좌석 선점(HOLD) 처리
        LocalDateTime expiresAt = now.plusMinutes(5);

        int pricePerSeat = schedule.getPerformance().getPrice();
        int totalAmount = pricePerSeat * seats.size();

        String orderId = "ORDER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String orderName = schedule.getPerformance().getTitle() + " " + seats.size() + "매 (" + String.join(", ", sortedSeatNumbers) + ")";

        Reservation reservation = new Reservation(member, schedule, orderId, orderName, totalAmount);
        for (Seat seat : seats) {
            seat.hold(expiresAt, reservation.getHoldToken());
            reservation.addReservationSeat(seat);
        }

        reservationRepository.save(reservation);
        log.info("[예매 생성 및 좌석 선점 완료] orderId={}, member={}, seats={}, totalAmount={}, expiresAt={}",
                orderId, member.getEmail(), sortedSeatNumbers, totalAmount, expiresAt);

        return ReservationResponse.from(reservation);
    }

    /**
     * 예매 취소 (사용자 명시적 취소 또는 이탈 시 선점 해제)
     */
    @Transactional
    public void cancelReservation(String memberEmail, String orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다: " + orderId));

        if (!reservation.getMember().getEmail().equals(memberEmail)) {
            throw new IllegalArgumentException("본인의 예매만 취소할 수 있습니다.");
        }

        if (reservation.getStatus() == com.example.backend.reservation.entity.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("이미 결제 완료된 예매는 일반 취소할 수 없습니다.");
        }

        reservation.cancel();
        List<Long> seatIds = reservation.getReservationSeats().stream()
                .map(rs -> rs.getSeat().getId()).distinct().sorted().toList();
        for (Long seatId : seatIds) {
            seatRepository.releaseHeldSeat(seatId, reservation.getHoldToken());
        }
        log.info("[예매 취소 및 좌석 해제 완료] orderId={}, member={}", orderId, memberEmail);
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
