package com.mi.bms.rule.interfaces.rest;

import com.mi.bms.rule.application.RuleService;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleResponse;
import com.mi.bms.shared.web.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "规则管理")
@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @ApiOperation("创建规则")
    @PostMapping
    public ApiResponse<WarnRuleResponse> createRule(@Valid @RequestBody WarnRuleRequest request) {
        return ApiResponse.success(ruleService.createRule(request));
    }

    @ApiOperation("更新规则")
    @PutMapping("/{ruleId}")
    public ApiResponse<WarnRuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody WarnRuleRequest request) {
        return ApiResponse.success(ruleService.updateRule(ruleId, request));
    }

    @ApiOperation("删除规则")
    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
        return ApiResponse.success();
    }

    @ApiOperation("根据ID查询规则")
    @GetMapping("/{ruleId}")
    public ApiResponse<WarnRuleResponse> getRuleById(@PathVariable Long ruleId) {
        return ApiResponse.success(ruleService.getRuleById(ruleId));
    }

    @ApiOperation("查询规则列表")
    @GetMapping
    public ApiResponse<List<WarnRuleResponse>> findRules(
            @ApiParam(value = "规则编号") @RequestParam(required = false) Integer ruleNo,
            @ApiParam(value = "电池类型编码") @RequestParam(required = false) String batteryTypeCode) {
        return ApiResponse.success(ruleService.findRules(ruleNo, batteryTypeCode));
    }
}