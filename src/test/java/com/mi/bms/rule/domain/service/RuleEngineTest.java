package com.mi.bms.rule.domain.service;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRule.RuleCondition;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleEngineTest {

    @Mock
    private WarnRuleRepository ruleRepository;

    private RuleEngine ruleEngine;
    private WarnRule voltageDiffRule;
    private WarnRule currentDiffRule;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(ruleRepository);

        // 创建电压差规则（三元电池）
        voltageDiffRule = WarnRule.create(1, "电压差报警", "MX_MI", 1); // 三元电池

        // 添加规则区间项
        addRuleCondition(voltageDiffRule, new BigDecimal("5"), null, 0); // 差值 >= 5，预警级别 0
        addRuleCondition(voltageDiffRule, new BigDecimal("3"), new BigDecimal("5"), 1); // 3 <= 差值 < 5，预警级别 1
        addRuleCondition(voltageDiffRule, new BigDecimal("1"), new BigDecimal("3"), 2); // 1 <= 差值 < 3，预警级别 2
        addRuleCondition(voltageDiffRule, new BigDecimal("0.6"), new BigDecimal("1"), 3); // 0.6 <= 差值 < 1，预警级别 3
        addRuleCondition(voltageDiffRule, new BigDecimal("0.2"), new BigDecimal("0.6"), 4); // 0.2 <= 差值 < 0.6，预警级别 4

        // 创建电流差规则（三元电池）
        currentDiffRule = WarnRule.create(2, "电流差报警", "IX_II", 1); // 三元电池

        // 添加规则区间项
        addRuleCondition(currentDiffRule, new BigDecimal("3"), null, 0); // 差值 >= 3，预警级别 0
        addRuleCondition(currentDiffRule, new BigDecimal("1"), new BigDecimal("3"), 1); // 1 <= 差值 < 3，预警级别 1
        addRuleCondition(currentDiffRule, new BigDecimal("0.2"), new BigDecimal("1"), 2); // 0.2 <= 差值 < 1，预警级别 2

        // Mock repository behavior for all test cases
        when(ruleRepository.findByRuleNoAndBatteryTypeId(1, 1)).thenReturn(List.of(voltageDiffRule));
        when(ruleRepository.findByRuleNoAndBatteryTypeId(2, 1)).thenReturn(List.of(currentDiffRule));
        when(ruleRepository.findByRuleNoAndBatteryTypeId(3, 1)).thenReturn(List.of());
    }

    private void addRuleCondition(WarnRule rule, BigDecimal minVal, BigDecimal maxVal, Integer warnLevel) {
        RuleCondition condition = RuleCondition.create(minVal, maxVal, warnLevel);
        rule.addCondition(condition);
    }

    @Test
    void evaluateSignal_VoltageDiff_Level0() {
        // Given - 电压差 = 6.0
        BigDecimal value = new BigDecimal("6.0");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(1, 1, value);

        // Then
        assertTrue(result.isPresent());
        assertEquals(0, result.get().getWarnLevel());
    }

    @Test
    void evaluateSignal_VoltageDiff_Level1() {
        // Given - 电压差 = 4.0
        BigDecimal value = new BigDecimal("4.0");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(1, 1, value);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getWarnLevel());
    }

    @Test
    void evaluateSignal_CurrentDiff_Level0() {
        // Given - 电流差 = 4.0
        BigDecimal value = new BigDecimal("4.0");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(2, 1, value);

        // Then
        assertTrue(result.isPresent());
        assertEquals(0, result.get().getWarnLevel());
    }

    @Test
    void evaluateSignal_CurrentDiff_Level1() {
        // Given - 电流差 = 2.0
        BigDecimal value = new BigDecimal("2.0");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(2, 1, value);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getWarnLevel());
    }

    @Test
    void evaluateSignal_NoMatchingRule() {
        // Given - 规则不存在
        BigDecimal value = new BigDecimal("1.0");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(3, 1, value);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void evaluateSignal_NoMatchingCondition() {
        // Given - 电压差 = 0.1 (小于所有区间)
        BigDecimal value = new BigDecimal("0.1");

        // When
        Optional<RuleCondition> result = ruleEngine.evaluateSignal(1, 1, value);

        // Then
        assertFalse(result.isPresent());
    }
}