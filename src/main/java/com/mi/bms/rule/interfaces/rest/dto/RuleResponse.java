package com.mi.bms.rule.interfaces.rest.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RuleResponse {

    private Long id;
    private Integer ruleNo;
    private String name;
    private String expr;
    private Integer batteryTypeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RuleItemResponse> conditions;
}