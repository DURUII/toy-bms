package com.mi.bms.rule.application.impl;

import com.mi.bms.rule.application.RuleService;
import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRule.RuleCondition;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.rule.interfaces.rest.dto.RuleItemRequest;
import com.mi.bms.rule.interfaces.rest.dto.RuleItemResponse;
import com.mi.bms.rule.interfaces.rest.dto.RuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.RuleResponse;
import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.ArrayList;
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
                                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", "id",
                                                request.getBatteryTypeId()));

                // Create rule
                WarnRule rule = WarnRule.create(
                                request.getRuleNo(),
                                request.getName(),
                                request.getExpr(),
                                batteryType.getId());

                // Add conditions
                for (RuleItemRequest conditionRequest : request.getConditions()) {
                        RuleCondition condition = RuleCondition.create(
                                        conditionRequest.getMinVal(),
                                        conditionRequest.getMaxVal(),
                                        conditionRequest.getWarnLevel());
                        rule.addCondition(condition);
                }

                // Save rule
                rule = ruleRepository.save(rule);

                // Build response
                return buildRuleResponse(rule);
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
                                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", "id",
                                                request.getBatteryTypeId()));

                // Update basic info
                rule.updateBasicInfo(request.getName(), request.getExpr());

                // Update conditions
                rule.clearConditions();
                for (RuleItemRequest conditionRequest : request.getConditions()) {
                        RuleCondition condition = RuleCondition.create(
                                        conditionRequest.getMinVal(),
                                        conditionRequest.getMaxVal(),
                                        conditionRequest.getWarnLevel());
                        rule.addCondition(condition);
                }

                // Save rule
                rule = ruleRepository.save(rule);

                // Build response
                return buildRuleResponse(rule);
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
                WarnRule rule = ruleRepository.findByIdWithItems(ruleId)
                                .orElseThrow(() -> new ResourceNotFoundException("WarnRule", "id", ruleId));
                return buildRuleResponse(rule);
        }

        @Override
        @Transactional(readOnly = true)
        public List<RuleResponse> getAllRules() {
                return ruleRepository.findAll().stream()
                                .map(this::buildRuleResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<RuleResponse> getRulesByRuleNoAndBatteryTypeId(Integer ruleNo, Integer batteryTypeId) {
                return ruleRepository.findByRuleNoAndBatteryTypeIdWithItems(ruleNo, batteryTypeId).stream()
                                .map(this::buildRuleResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<RuleResponse> getRulesByBatteryTypeId(Integer batteryTypeId) {
                return ruleRepository.findByBatteryTypeId(batteryTypeId).stream()
                                .map(this::buildRuleResponse)
                                .collect(Collectors.toList());
        }

        private RuleResponse buildRuleResponse(WarnRule rule) {
                RuleResponse response = new RuleResponse();
                response.setId(rule.getId());
                response.setRuleNo(rule.getRuleNo());
                response.setName(rule.getName());
                response.setExpr(rule.getExpr());
                response.setBatteryTypeId(rule.getBatteryTypeId());
                response.setCreatedAt(rule.getCreatedAt());
                response.setUpdatedAt(rule.getUpdatedAt());

                List<RuleItemResponse> itemResponses = new ArrayList<>();
                for (RuleCondition condition : rule.getConditions()) {
                        RuleItemResponse itemResponse = new RuleItemResponse();
                        itemResponse.setId(condition.getId());
                        itemResponse.setMinVal(condition.getMinVal());
                        itemResponse.setMaxVal(condition.getMaxVal());
                        itemResponse.setWarnLevel(condition.getWarnLevel());
                        itemResponses.add(itemResponse);
                }
                response.setConditions(itemResponses);

                return response;
        }
}