package com.mi.bms.vehicle.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private String vid;
    private Integer carId;
    private String batteryTypeCode;
    private String batteryTypeName;
    private Long mileageKm;
    private Integer healthPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}