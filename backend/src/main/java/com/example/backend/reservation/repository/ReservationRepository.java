package com.example.backend.reservation.repository;

import com.example.backend.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("select distinct r from Reservation r " +
           "join fetch r.schedule s " +
           "join fetch s.performance p " +
           "left join fetch r.reservationSeats rs " +
           "left join fetch rs.seat " +
           "where r.member.id = :memberId " +
           "order by r.createdAt desc")
    List<Reservation> findWithDetailsByMemberId(@Param("memberId") Long memberId);

    List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Reservation> findByOrderId(String orderId);
}
