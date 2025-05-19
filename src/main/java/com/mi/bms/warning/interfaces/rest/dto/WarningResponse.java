package com.mi.bms.warning.interfaces.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarningResponse {
    private Long warningId;
    private Integer carId;
    private Integer batteryTypeId;
    private String batteryTypeCode;
    private String batteryTypeName;
    private Integer ruleNo;
    private String ruleName;
    private Integer warnLevel;
    private String signalData;
    private LocalDateTime createdAt;
}