package com.hs.railway_stats.repository.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "trip_info_metric",
    uniqueConstraints = @UniqueConstraint(columnNames = {"origin_id", "destination_id", "scheduled_departure_time"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripInfoMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer originId;

    @Column(nullable = false)
    private Integer destinationId;

    @Column(nullable = false)
    private LocalTime scheduledDepartureTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalTrips = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer averageMinutesLate = 0;

    @ElementCollection
    @CollectionTable(name = "trip_info_metric_canceled_dates", joinColumns = @JoinColumn(name = "metric_id"))
    @Column(name = "canceled_date", nullable = false)
    @Builder.Default
    private List<LocalDate> canceledTripDates = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer totalReimbursableTrips = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalReimbursementsRequested = 0;

    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Stockholm"));
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now(ZoneId.of("Europe/Stockholm"));
    }
}
