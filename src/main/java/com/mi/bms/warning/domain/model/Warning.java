package com.mi.bms.warning.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warnings")
@Where(clause = "is_delete = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer carId;

    @Column(nullable = false)
    private Integer batteryTypeId;

    @Column(nullable = false)
    private Integer ruleNo;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private Integer warnLevel;

    @Column(nullable = false)
    private String signalData;

    @Column(nullable = false)
    private boolean isDelete;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        isDelete = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Warning create(Integer carId, Integer batteryTypeId, Integer ruleNo,
            String ruleName, Integer warnLevel, String signalData) {
        Warning warning = new Warning();
        warning.carId = carId;
        warning.batteryTypeId = batteryTypeId;
        warning.ruleNo = ruleNo;
        warning.ruleName = ruleName;
        warning.warnLevel = warnLevel;
        warning.signalData = signalData;
        return warning;
    }

    public void markAsDeleted() {
        this.isDelete = true;
        this.updatedAt = LocalDateTime.now();
    }
}