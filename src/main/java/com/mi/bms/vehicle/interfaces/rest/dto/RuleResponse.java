package com.mi.bms.vehicle.interfaces.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
public class RuleResponse {
    private Long ruleId;
    private Integer ruleNo;
    private String name;
    private String expr;
    private Integer batteryTypeId;
    private String batteryTypeCode;
    private String batteryTypeName;
    private List<Condition> conditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class Condition {
        private Long itemId;
        private BigDecimal minVal;
        private BigDecimal maxVal;
        private Integer warnLevel;
    }
}