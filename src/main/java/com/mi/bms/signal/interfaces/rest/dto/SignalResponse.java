package com.mi.bms.signal.interfaces.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SignalResponse {
    private Long signalId;
    private Integer carId;
    private Integer batteryTypeId;
    private String batteryTypeCode;
    private String batteryTypeName;
    private String signalData;
    private boolean processed;
    private LocalDateTime createdAt;
}