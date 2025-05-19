package com.mi.bms.rule.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleItemRequest {

    private Long id;

    private BigDecimal minVal;

    private BigDecimal maxVal;

    @NotNull(message = "预警级别不能为空")
    private Integer warnLevel;
}