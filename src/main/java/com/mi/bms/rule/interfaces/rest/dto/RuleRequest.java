package com.mi.bms.rule.interfaces.rest.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RuleRequest {

    @NotNull(message = "规则编号不能为空")
    private Integer ruleNo;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 64, message = "规则名称长度不能超过64个字符")
    private String name;

    @NotBlank(message = "规则表达式不能为空")
    @Size(max = 32, message = "规则表达式长度不能超过32个字符")
    private String expr;

    @NotNull(message = "电池类型ID不能为空")
    private Integer batteryTypeId;

    @NotEmpty(message = "规则条件不能为空")
    @Valid
    private List<RuleItemRequest> conditions;
}