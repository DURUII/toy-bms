package com.mi.bms.vehicle.interfaces.rest.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class RuleConditionRequest {
    private BigDecimal minVal;
    private BigDecimal maxVal;

    @NotNull(message = "告警等级不能为空")
    private Integer warnLevel;
}