package com.example.backend.reservation.repository;

import com.example.backend.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Reservation> findByOrderId(String orderId);
}
