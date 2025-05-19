package com.mi.bms.rule.interfaces.rest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RuleItemResponse {
    
    private Long id;
    private BigDecimal minVal;
    private BigDecimal maxVal;
    private Integer warnLevel;
}