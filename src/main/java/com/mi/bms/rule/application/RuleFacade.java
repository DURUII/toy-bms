package com.mi.bms.rule.application;

import com.mi.bms.rule.domain.model.WarnRule;

import java.util.List;

/**
 * 规则门面，供其他上下文使用
 */
public interface RuleFacade {

    /**
     * 根据规则编号和电池类型获取规则列表
     */
    List<WarnRule> findRules(Integer ruleNo, Integer batteryTypeId);

    /**
     * 根据规则ID列表获取规则
     */
    List<WarnRule> findRulesByIds(List<Long> ruleIds);

    /**
     * 根据规则ID获取规则
     */
    WarnRule findRuleById(Long ruleId);
}