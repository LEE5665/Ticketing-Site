package com.example.backend.reservation.repository;

import com.example.backend.reservation.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    List<ReservationSeat> findByReservationId(Long reservationId);
}
