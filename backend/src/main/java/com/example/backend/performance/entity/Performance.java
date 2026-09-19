package com.example.backend.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "performances")
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subtitle;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String dateText;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String tag;

    @Column(nullable = false)
    private String symbol;

    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceSchedule> schedules = new ArrayList<>();

    public Performance(String slug, String title, String subtitle, String category,
                       String venue, String dateText, int price, String color,
                       String tag, String symbol) {
        this.slug = slug;
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.venue = venue;
        this.dateText = dateText;
        this.price = price;
        this.color = color;
        this.tag = tag;
        this.symbol = symbol;
    }
}
