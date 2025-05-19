package com.mi.bms.rule.interfaces.rest.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class RuleItemRequest {

    @NotNull(message = "最小值不能为空")
    private BigDecimal minVal;

    @NotNull(message = "最大值不能为空")
    private BigDecimal maxVal;

    @NotNull(message = "警告等级不能为空")
    @Min(value = 1, message = "警告等级最小为1")
    @Max(value = 5, message = "警告等级最大为5")
    private Integer warnLevel;
}