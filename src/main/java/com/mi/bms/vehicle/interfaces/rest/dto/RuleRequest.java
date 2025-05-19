package com.mi.bms.vehicle.interfaces.rest.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RuleRequest {
    @NotNull(message = "规则编号不能为空")
    private Integer ruleNo;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "规则表达式不能为空")
    private String expr;

    @NotNull(message = "电池类型ID不能为空")
    private Integer batteryTypeId;

    private List<RuleConditionRequest> conditions;
}