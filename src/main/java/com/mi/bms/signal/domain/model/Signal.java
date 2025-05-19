package com.mi.bms.signal.domain.model;

import com.mi.bms.shared.domain.ValueObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "signal")
@Where(clause = "is_delete = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Signal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer carId;

    @Column(nullable = false)
    private Integer batteryTypeId;

    @Column(nullable = false)
    private String signalData;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private boolean isDelete;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Use the embedded values for calculation, but don't persist them
    @Transient
    private SignalValues values;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        isDelete = false;
        processed = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Signal create(Integer carId, Integer batteryTypeId, Map<String, BigDecimal> signalValues) {
        Signal signal = new Signal();
        signal.carId = carId;
        signal.batteryTypeId = batteryTypeId;
        signal.values = SignalValues.create(signalValues);
        signal.signalData = signal.values.toJson();
        return signal;
    }

    public void markAsProcessed() {
        this.processed = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.isDelete = true;
        this.updatedAt = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SignalValues implements ValueObject {
        private BigDecimal maxVoltage; // Mx
        private BigDecimal minVoltage; // Mi
        private BigDecimal maxCurrent; // Ix
        private BigDecimal minCurrent; // Ii

        public static SignalValues create(Map<String, BigDecimal> values) {
            SignalValues signalValues = new SignalValues();
            signalValues.maxVoltage = values.get("Mx");
            signalValues.minVoltage = values.get("Mi");
            signalValues.maxCurrent = values.get("Ix");
            signalValues.minCurrent = values.get("Ii");
            return signalValues;
        }

        public String toJson() {
            Map<String, BigDecimal> map = new HashMap<>();
            if (maxVoltage != null)
                map.put("Mx", maxVoltage);
            if (minVoltage != null)
                map.put("Mi", minVoltage);
            if (maxCurrent != null)
                map.put("Ix", maxCurrent);
            if (minCurrent != null)
                map.put("Ii", minCurrent);
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize signal values", e);
            }
        }

        public BigDecimal getVoltageDiff() {
            if (maxVoltage == null || minVoltage == null) {
                return null;
            }
            return maxVoltage.subtract(minVoltage);
        }

        public BigDecimal getCurrentDiff() {
            if (maxCurrent == null || minCurrent == null) {
                return null;
            }
            return maxCurrent.subtract(minCurrent);
        }
    }
}