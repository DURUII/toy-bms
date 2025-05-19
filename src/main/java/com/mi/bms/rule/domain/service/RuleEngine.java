package com.mi.bms.rule.domain.service;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRule.RuleCondition;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 规则引擎，用于评估信号是否触发预警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final WarnRuleRepository ruleRepository;

    public Optional<RuleCondition> evaluateSignal(Integer ruleNo, Integer batteryTypeId, BigDecimal value) {
        log.debug("Evaluating signal for ruleNo: {}, batteryTypeId: {}, value: {}", ruleNo, batteryTypeId, value);

        // Get rules for the given rule number and battery type
        List<WarnRule> rules = ruleRepository.findByRuleNoAndBatteryTypeId(ruleNo, batteryTypeId);
        if (rules.isEmpty()) {
            log.warn("No rules found for ruleNo: {}, batteryTypeId: {}", ruleNo, batteryTypeId);
            return Optional.empty();
    }

        // Evaluate each rule's conditions
        for (WarnRule rule : rules) {
            for (RuleCondition condition : rule.getConditions()) {
                if (condition.isInRange(value)) {
                    log.debug("Found matching condition: {}", condition);
                    return Optional.of(condition);
                }
            }
        }

        log.debug("No matching conditions found for value: {}", value);
        return Optional.empty();
    }

    public List<WarnRule> getRulesForBatteryType(Integer batteryTypeId) {
        return ruleRepository.findByBatteryTypeId(batteryTypeId);
    }

    public WarnRule getRuleById(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WarnRule", "id", ruleId));
    }
}