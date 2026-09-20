package com.example.backend.seat.entity;

import com.example.backend.performance.entity.PerformanceSchedule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_seat_number",
                        columnNames = {"schedule_id", "seat_number"}
                )
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    private LocalDateTime holdExpiresAt;

    private String holdToken;

    @Version
    private Long version;

    public Seat(PerformanceSchedule schedule, String seatNumber, SeatStatus status) {
        this.schedule = schedule;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    public boolean isAvailable(LocalDateTime now) {
        if (this.status == SeatStatus.AVAILABLE) {
            return true;
        }
        // HOLD 상태인데 만료 시간이 지났으면 다시 선택 가능한 좌석
        return this.status == SeatStatus.HOLD
                && this.holdExpiresAt != null
                && this.holdExpiresAt.isBefore(now);
    }

    public void hold(LocalDateTime expiresAt, String holdToken) {
        this.status = SeatStatus.HOLD;
        this.holdExpiresAt = expiresAt;
        this.holdToken = holdToken;
    }

    public void reserve() {
        this.status = SeatStatus.RESERVED;
        this.holdExpiresAt = null;
        this.holdToken = null;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.holdExpiresAt = null;
        this.holdToken = null;
    }
}
