package com.example.backend.performance.repository;

import com.example.backend.performance.entity.PerformanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {
    List<PerformanceSchedule> findByPerformanceIdOrderByStartTimeAsc(Long performanceId);
}
