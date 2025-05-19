package com.mi.bms.rule.application.impl;

import com.mi.bms.rule.application.RuleFacade;
import com.mi.bms.rule.application.RuleService;
import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRuleItem;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.rule.interfaces.rest.dto.RuleItemResponse;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleResponse;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService, RuleFacade {

    private static final String RULES_CACHE_PREFIX = "rules:";
    private static final String RULE_KEY_PREFIX = "rule:";

    private final WarnRuleRepository ruleRepository;
    private final BatteryTypeRepository batteryTypeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    @CacheEvict(value = "rules", allEntries = true)
    public WarnRuleResponse createRule(WarnRuleRequest request) {
        // 1. 获取电池类型
        BatteryType batteryType = getBatteryTypeByCode(request.getBatteryTypeCode());

        // 2. 创建规则实体
        WarnRule rule = WarnRule.builder()
                .ruleNo(request.getRuleNo())
                .name(request.getName())
                .expr(request.getExpr())
                .batteryTypeId(batteryType.getId())
                .build();

        // 3. 添加规则项
        for (var itemRequest : request.getItems()) {
            WarnRuleItem item = WarnRuleItem.builder()
                    .minVal(itemRequest.getMinVal())
                    .maxVal(itemRequest.getMaxVal())
                    .warnLevel(itemRequest.getWarnLevel())
                    .build();
            rule.addItem(item);
        }

        // 4. 保存规则
        WarnRule savedRule = ruleRepository.save(rule);

        // 5. 清除Redis缓存
        evictRuleCache(savedRule.getRuleNo(), batteryType.getId());

        log.info("Rule created: {}", savedRule.getId());

        return buildWarnRuleResponse(savedRule, batteryType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "rules", allEntries = true)
    public WarnRuleResponse updateRule(Long ruleId, WarnRuleRequest request) {
        // 1. 查找规则
        WarnRule rule = ruleRepository.findByIdWithItems(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId.toString()));

        // 2. 获取电池类型
        BatteryType batteryType = getBatteryTypeByCode(request.getBatteryTypeCode());

        // 3. 更新规则基本信息
        rule.setRuleNo(request.getRuleNo());
        rule.setName(request.getName());
        rule.setExpr(request.getExpr());
        rule.setBatteryTypeId(batteryType.getId());

        // 4. 清除所有旧规则项并添加新规则项
        rule.clearItems();

        for (var itemRequest : request.getItems()) {
            WarnRuleItem item = WarnRuleItem.builder()
                    .minVal(itemRequest.getMinVal())
                    .maxVal(itemRequest.getMaxVal())
                    .warnLevel(itemRequest.getWarnLevel())
                    .build();
            rule.addItem(item);
        }

        // 5. 保存规则
        WarnRule updatedRule = ruleRepository.save(rule);

        // 6. 清除Redis缓存
        evictRuleCache(updatedRule.getRuleNo(), batteryType.getId());

        log.info("Rule updated: {}", updatedRule.getId());

        return buildWarnRuleResponse(updatedRule, batteryType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "rules", allEntries = true)
    public void deleteRule(Long ruleId) {
        // 1. 查找规则
        WarnRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId.toString()));

        // 2. 逻辑删除规则
        rule.setIsDelete(true);
        ruleRepository.save(rule);

        // 3. 清除Redis缓存
        evictRuleCache(rule.getRuleNo(), rule.getBatteryTypeId());

        log.info("Rule deleted: {}", ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public WarnRuleResponse getRuleById(Long ruleId) {
        // 1. 查找规则
        WarnRule rule = ruleRepository.findByIdWithItems(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId.toString()));

        // 2. 获取电池类型
        BatteryType batteryType = getBatteryTypeById(rule.getBatteryTypeId());

        return buildWarnRuleResponse(rule, batteryType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarnRuleResponse> findRules(Integer ruleNo, String batteryTypeCode) {
        List<WarnRule> rules;

        // 处理过滤条件
        if (ruleNo != null && batteryTypeCode != null) {
            // 根据规则编号和电池类型查询
            BatteryType batteryType = getBatteryTypeByCode(batteryTypeCode);
            rules = ruleRepository.findByRuleNoAndBatteryTypeIdWithItems(ruleNo, batteryType.getId());
        } else if (ruleNo != null) {
            // 仅根据规则编号查询
            rules = ruleRepository.findByRuleNo(ruleNo);
        } else if (batteryTypeCode != null) {
            // 仅根据电池类型查询
            BatteryType batteryType = getBatteryTypeByCode(batteryTypeCode);
            rules = ruleRepository.findByBatteryTypeId(batteryType.getId());
        } else {
            // 查询所有规则
            rules = ruleRepository.findAll();
        }

        // 转换为DTO并返回
        return rules.stream()
                .map(rule -> {
                    BatteryType batteryType = getBatteryTypeById(rule.getBatteryTypeId());
                    return buildWarnRuleResponse(rule, batteryType);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "rules", key = "'ruleNo:' + #ruleNo + ':batteryType:' + #batteryTypeId")
    public List<WarnRule> findRuleEntities(Integer ruleNo, Integer batteryTypeId) {
        log.debug("Fetching rules from DB for ruleNo: {}, batteryTypeId: {}", ruleNo, batteryTypeId);
        return ruleRepository.findByRuleNoAndBatteryTypeIdWithItems(ruleNo, batteryTypeId);
    }

    @Override
    public List<WarnRule> findRules(Integer ruleNo, Integer batteryTypeId) {
        return findRuleEntities(ruleNo, batteryTypeId);
    }

    @Override
    public List<WarnRule> findRulesByIds(List<Long> ruleIds) {
        return ruleIds.stream()
                .map(this::findRuleById)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "rules", key = "'id:' + #ruleId")
    public WarnRule findRuleById(Long ruleId) {
        return ruleRepository.findByIdWithItems(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId.toString()));
    }

    // 辅助方法

    private void evictRuleCache(Integer ruleNo, Integer batteryTypeId) {
        String ruleKey = RULES_CACHE_PREFIX + "ruleNo:" + ruleNo + ":batteryType:" + batteryTypeId;
        redisTemplate.delete(ruleKey);
    }

    private BatteryType getBatteryTypeByCode(String code) {
        return batteryTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", code));
    }

    private BatteryType getBatteryTypeById(Integer id) {
        return batteryTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", id.toString()));
    }

    private WarnRuleResponse buildWarnRuleResponse(WarnRule rule, BatteryType batteryType) {
        List<RuleItemResponse> itemResponses = rule.getItems().stream()
                .map(item -> RuleItemResponse.builder()
                        .id(item.getId())
                        .minVal(item.getMinVal())
                        .maxVal(item.getMaxVal())
                        .warnLevel(item.getWarnLevel())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return WarnRuleResponse.builder()
                .id(rule.getId())
                .ruleNo(rule.getRuleNo())
                .name(rule.getName())
                .expr(rule.getExpr())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .items(itemResponses)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}