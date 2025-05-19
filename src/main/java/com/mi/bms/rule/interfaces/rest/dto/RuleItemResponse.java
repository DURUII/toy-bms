package com.mi.bms.rule.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleItemResponse {

    private Long id;
    private BigDecimal minVal;
    private BigDecimal maxVal;
    private Integer warnLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}