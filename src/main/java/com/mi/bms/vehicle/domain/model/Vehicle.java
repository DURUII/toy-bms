package com.mi.bms.vehicle.domain.model;

import com.mi.bms.shared.domain.ValueObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle")
@Where(clause = "is_delete = 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Vehicle {

    @Id
    @Column(name = "vid", length = 16)
    @Getter
    private String vid;

    @Column(name = "car_id", nullable = false, unique = true)
    @Getter
    private Integer carId;

    @Column(name = "battery_type_id", nullable = false)
    @Getter
    private Integer batteryTypeId;

    @Embedded
    @Getter
    private VehicleStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter
    private LocalDateTime updatedAt;

    @Column(name = "is_delete")
    private Boolean isDelete = false;

    // Factory method for creating new vehicles
    public static Vehicle create(String vid, Integer carId, Integer batteryTypeId, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.vid = vid;
        vehicle.carId = carId;
        vehicle.batteryTypeId = batteryTypeId;
        vehicle.status = status;
        return vehicle;
    }

    // Business logic methods
    public void updateStatus(VehicleStatus newStatus) {
        validateStatus(newStatus);
        this.status = newStatus;
    }

    public void markAsDeleted() {
        this.isDelete = true;
    }

    public boolean isDeleted() {
        return isDelete;
    }

    private void validateStatus(VehicleStatus status) {
        if (status.getMileageKm() < 0) {
            throw new IllegalArgumentException("Mileage cannot be negative");
        }
        if (status.getHealthPct() < 0 || status.getHealthPct() > 100) {
            throw new IllegalArgumentException("Health percentage must be between 0 and 100");
        }
    }

    // Value object for vehicle status
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class VehicleStatus implements ValueObject {
        @Column(name = "mileage_km", nullable = false)
        @Getter
        private Long mileageKm;

        @Column(name = "health_pct", nullable = false)
        @Getter
        private Integer healthPct;

        public static VehicleStatus create(Long mileageKm, Integer healthPct) {
            VehicleStatus status = new VehicleStatus();
            status.mileageKm = mileageKm;
            status.healthPct = healthPct;
            return status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof VehicleStatus))
                return false;
            VehicleStatus that = (VehicleStatus) o;
            return mileageKm.equals(that.mileageKm) && healthPct.equals(that.healthPct);
        }

        @Override
        public int hashCode() {
            return 31 * mileageKm.hashCode() + healthPct.hashCode();
        }
    }
}