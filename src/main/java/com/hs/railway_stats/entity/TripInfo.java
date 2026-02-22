package com.hs.railway_stats.entity;

import java.time.LocalDateTime;

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
    private LocalDateTime originalDepartureTime;
    
    @Column
    private LocalDateTime actualArrivalTime;
    
    @Column
    private Integer canceled;
    
    @Column
    private Integer minutesLate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
