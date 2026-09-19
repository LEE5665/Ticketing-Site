package com.example.backend.performance.repository;

import com.example.backend.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    Optional<Performance> findBySlug(String slug);
}
