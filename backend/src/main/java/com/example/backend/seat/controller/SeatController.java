package com.example.backend.seat.controller;

import com.example.backend.seat.dto.SeatResponse;
import com.example.backend.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatRepository seatRepository;

    @GetMapping
    public List<SeatResponse> getSeats(@PathVariable Long scheduleId) {
        LocalDateTime now = LocalDateTime.now();
        return seatRepository.findByScheduleIdOrderBySeatNumberAsc(scheduleId).stream()
                .map(seat -> SeatResponse.from(seat, now))
                .toList();
    }
}
