package com.example.backend.seat.repository;

import com.example.backend.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleIdOrderBySeatNumberAsc(Long scheduleId);
    Optional<Seat> findByScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);
}
