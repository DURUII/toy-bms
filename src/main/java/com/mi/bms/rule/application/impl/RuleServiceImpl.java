package com.mi.bms.rule.application.impl;

import com.mi.bms.rule.application.RuleService;
import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRule.RuleCondition;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleConditionRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final WarnRuleRepository ruleRepository;
    private final BatteryTypeRepository batteryTypeRepository;

    @Override
    @Transactional
    public RuleResponse createRule(@Valid RuleRequest request) {
        // Check if rule number already exists
        if (ruleRepository.existsByRuleNo(request.getRuleNo())) {
            throw new BusinessException("ALREADY_EXISTS", "规则编号已存在");
        }

        // Get battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", "id", request.getBatteryTypeId()));

        // Create rule
        WarnRule rule = WarnRule.create(
                request.getRuleNo(),
                request.getName(),
                request.getExpr(),
                batteryType.getId());

        // Add conditions
        for (RuleConditionRequest conditionRequest : request.getConditions()) {
            RuleCondition condition = RuleCondition.create(
                    conditionRequest.getMinVal(),
                    conditionRequest.getMaxVal(),
                    conditionRequest.getWarnLevel());
            rule.addCondition(condition);
        }

        // Save rule
        rule = ruleRepository.save(rule);

        // Build response
        return buildRuleResponse(rule, batteryType);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(Long ruleId, @Valid RuleRequest request) {
        // Get existing rule
        WarnRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WarnRule", "id", ruleId));

        // Check if rule number is being changed and if it already exists
        if (!rule.getRuleNo().equals(request.getRuleNo()) &&
                ruleRepository.existsByRuleNo(request.getRuleNo())) {
            throw new BusinessException("ALREADY_EXISTS", "规则编号已存在");
        }

        // Get battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", "id", request.getBatteryTypeId()));

        // Update basic info
        rule.updateBasicInfo(request.getName(), request.getExpr());

        // Update conditions
        rule.clearConditions();
        for (RuleConditionRequest conditionRequest : request.getConditions()) {
            RuleCondition condition = RuleCondition.create(
                    conditionRequest.getMinVal(),
                    conditionRequest.getMaxVal(),
                    conditionRequest.getWarnLevel());
            rule.addCondition(condition);
        }

        // Save rule
        rule = ruleRepository.save(rule);

        // Build response
        return buildRuleResponse(rule, batteryType);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        WarnRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WarnRule", "id", ruleId));
        rule.markAsDeleted();
        ruleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleResponse getRuleById(Long ruleId) {
        WarnRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WarnRule", "id", ruleId));

        BatteryType batteryType = batteryTypeRepository.findById(rule.getBatteryTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", "id", rule.getBatteryTypeId()));

        return buildRuleResponse(rule, batteryType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(rule -> {
                    BatteryType batteryType = batteryTypeRepository.findById(rule.getBatteryTypeId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("BatteryType", "id", rule.getBatteryTypeId()));
                    return buildRuleResponse(rule, batteryType);
                })
                .collect(Collectors.toList());
    }

    private RuleResponse buildRuleResponse(WarnRule rule, BatteryType batteryType) {
        return RuleResponse.builder()
                .ruleId(rule.getId())
                .ruleNo(rule.getRuleNo())
                .name(rule.getName())
                .expr(rule.getExpr())
                .batteryTypeId(batteryType.getId())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .conditions(rule.getConditions().stream()
                        .map(condition -> RuleResponse.Condition.builder()
                                .itemId(condition.getId())
                                .minVal(condition.getMinVal())
                                .maxVal(condition.getMaxVal())
                                .warnLevel(condition.getWarnLevel())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}