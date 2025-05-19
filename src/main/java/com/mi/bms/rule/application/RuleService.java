package com.mi.bms.rule.application;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleResponse;

import java.util.List;

public interface RuleService {

    /**
     * 创建规则
     */
    WarnRuleResponse createRule(WarnRuleRequest request);

    /**
     * 更新规则
     */
    WarnRuleResponse updateRule(Long ruleId, WarnRuleRequest request);

    /**
     * 删除规则
     */
    void deleteRule(Long ruleId);

    /**
     * 根据ID查询规则
     */
    WarnRuleResponse getRuleById(Long ruleId);

    /**
     * 根据条件查询规则
     */
    List<WarnRuleResponse> findRules(Integer ruleNo, String batteryTypeCode);

    /**
     * 内部方法：根据规则编号和电池类型获取规则（含子项）
     */
    List<WarnRule> findRuleEntities(Integer ruleNo, Integer batteryTypeId);
}