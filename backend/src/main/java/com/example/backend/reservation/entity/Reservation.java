package com.example.backend.reservation.entity;

import com.example.backend.member.entity.Member;
import com.example.backend.performance.entity.PerformanceSchedule;
import com.example.backend.seat.entity.Seat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    private String holdToken;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    public Reservation(Member member, PerformanceSchedule schedule, String orderId, String orderName, int totalAmount) {
        this.member = member;
        this.schedule = schedule;
        this.orderId = orderId;
        this.holdToken = UUID.randomUUID().toString();
        this.orderName = orderName;
        this.totalAmount = totalAmount;
        this.status = ReservationStatus.PENDING_PAYMENT;
        this.createdAt = LocalDateTime.now();
    }

    public void addReservationSeat(Seat seat) {
        ReservationSeat rs = new ReservationSeat(this, seat);
        this.reservationSeats.add(rs);
    }

    public void confirm(String paymentKey) {
        this.status = ReservationStatus.CONFIRMED;
        this.paymentKey = paymentKey;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
