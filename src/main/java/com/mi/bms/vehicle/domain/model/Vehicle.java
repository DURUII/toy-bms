package com.mi.bms.vehicle.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle")
@Where(clause = "is_delete = 0")
public class Vehicle {

    @Id
    @Column(name = "vid", length = 16)
    private String vid;

    @Column(name = "car_id", nullable = false, unique = true)
    private Integer carId;

    @Column(name = "battery_type_id", nullable = false)
    private Integer batteryTypeId;

    @Column(name = "mileage_km", nullable = false)
    private Long mileageKm;

    @Column(name = "health_pct", nullable = false)
    private Integer healthPct;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_delete")
    private Boolean isDelete = false;
}