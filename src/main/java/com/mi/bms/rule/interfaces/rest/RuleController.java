package com.mi.bms.rule.interfaces.rest;

import com.mi.bms.rule.application.RuleService;
import com.mi.bms.rule.interfaces.rest.dto.RuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.RuleResponse;
import com.mi.bms.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "规则管理")
@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @Operation(summary = "创建规则")
    @PostMapping
    public ApiResponse<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        return ApiResponse.success(ruleService.createRule(request));
    }

    @Operation(summary = "根据ID获取规则")
    @GetMapping("/{id}")
    public ApiResponse<RuleResponse> getRuleById(
            @Parameter(description = "规则ID") @PathVariable Long id) {
        return ApiResponse.success(ruleService.getRuleById(id));
    }

    @Operation(summary = "根据规则编号和电池类型获取规则")
    @GetMapping("/ruleNo/{ruleNo}/batteryType/{batteryTypeId}")
    public ApiResponse<List<RuleResponse>> getRulesByRuleNoAndBatteryTypeId(
            @Parameter(description = "规则编号") @PathVariable Integer ruleNo,
            @Parameter(description = "电池类型ID") @PathVariable Integer batteryTypeId) {
        return ApiResponse.success(ruleService.getRulesByRuleNoAndBatteryTypeId(ruleNo, batteryTypeId));
    }

    @Operation(summary = "根据电池类型获取规则")
    @GetMapping("/batteryType/{batteryTypeId}")
    public ApiResponse<List<RuleResponse>> getRulesByBatteryTypeId(
            @Parameter(description = "电池类型ID") @PathVariable Integer batteryTypeId) {
        return ApiResponse.success(ruleService.getRulesByBatteryTypeId(batteryTypeId));
    }

    @Operation(summary = "获取所有规则")
    @GetMapping
    public ApiResponse<List<RuleResponse>> getAllRules() {
        return ApiResponse.success(ruleService.getAllRules());
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public ApiResponse<RuleResponse> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody RuleRequest request) {
        return ApiResponse.success(ruleService.updateRule(id, request));
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {
        ruleService.deleteRule(id);
        return ApiResponse.success();
    }
}