package com.hs.railway_stats.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "trip_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer originId;

    @Column(nullable = false)
    private Integer destinationId;

    @Column
    private ZonedDateTime originalDepartureTime;

    @Column
    private ZonedDateTime actualArrivalTime;

    @Column
    private Integer canceled;

    @Column
    private Integer minutesLate;

    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now(ZoneId.systemDefault());
    }
}
