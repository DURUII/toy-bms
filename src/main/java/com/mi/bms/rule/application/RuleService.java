package com.mi.bms.rule.application;

import com.mi.bms.vehicle.interfaces.rest.dto.RuleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleResponse;

import javax.validation.Valid;
import java.util.List;

public interface RuleService {

    /**
     * 创建规则
     */
    RuleResponse createRule(@Valid RuleRequest request);

    /**
     * 更新规则
     */
    RuleResponse updateRule(Long ruleId, @Valid RuleRequest request);

    /**
     * 删除规则
     */
    void deleteRule(Long ruleId);

    /**
     * 根据ID查询规则
     */
    RuleResponse getRuleById(Long ruleId);

    /**
     * 获取所有规则
     */
    List<RuleResponse> getAllRules();
}