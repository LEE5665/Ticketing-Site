package com.example.backend.performance.controller;

import com.example.backend.performance.dto.PerformanceResponse;
import com.example.backend.performance.entity.Performance;
import com.example.backend.performance.repository.PerformanceRepository;
import com.example.backend.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;

    @GetMapping
    public List<PerformanceResponse> getAllPerformances() {
        return performanceRepository.findAll().stream()
                .map(p -> PerformanceResponse.from(p, scheduleRepository.findByPerformanceIdOrderByStartTimeAsc(p.getId())))
                .toList();
    }

    @GetMapping("/{slug}")
    public PerformanceResponse getPerformanceBySlug(@PathVariable String slug) {
        Performance performance = performanceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."));

        return PerformanceResponse.from(
                performance,
                scheduleRepository.findByPerformanceIdOrderByStartTimeAsc(performance.getId())
        );
    }
}
