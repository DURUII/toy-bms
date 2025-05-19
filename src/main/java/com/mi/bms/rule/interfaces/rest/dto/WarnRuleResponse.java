package com.mi.bms.rule.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarnRuleResponse {

    private Long id;
    private Integer ruleNo;
    private String name;
    private String expr;
    private String batteryTypeCode;
    private String batteryTypeName;
    private List<RuleItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}