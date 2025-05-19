package com.mi.bms.rule.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarnRuleRequest {

    private Long ruleId;

    @NotNull(message = "规则编号不能为空")
    private Integer ruleNo;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "表达式不能为空")
    private String expr;

    @NotBlank(message = "电池类型编码不能为空")
    private String batteryTypeCode;

    @NotEmpty(message = "规则项不能为空")
    @Valid
    private List<RuleItemRequest> items;
}