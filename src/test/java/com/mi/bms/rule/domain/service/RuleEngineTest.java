package com.mi.bms.rule.domain.service;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRuleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RuleEngineTest {

    private RuleEngine ruleEngine;
    private WarnRule voltageDiffRule;
    private WarnRule currentDiffRule;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();

        // 创建电压差规则（三元电池）
        voltageDiffRule = WarnRule.builder()
                .id(1L)
                .ruleNo(1)
                .name("电压差报警")
                .expr("MX_MI") // 最高电压 - 最低电压
                .batteryTypeId(1) // 三元电池
                .items(new ArrayList<>())
                .build();

        // 添加规则区间项
        addRuleItem(voltageDiffRule, new BigDecimal("5"), null, 0); // 差值 >= 5，预警级别 0
        addRuleItem(voltageDiffRule, new BigDecimal("3"), new BigDecimal("5"), 1); // 3 <= 差值 < 5，预警级别 1
        addRuleItem(voltageDiffRule, new BigDecimal("1"), new BigDecimal("3"), 2); // 1 <= 差值 < 3，预警级别 2
        addRuleItem(voltageDiffRule, new BigDecimal("0.6"), new BigDecimal("1"), 3); // 0.6 <= 差值 < 1，预警级别 3
        addRuleItem(voltageDiffRule, new BigDecimal("0.2"), new BigDecimal("0.6"), 4); // 0.2 <= 差值 < 0.6，预警级别 4

        // 创建电流差规则（三元电池）
        currentDiffRule = WarnRule.builder()
                .id(2L)
                .ruleNo(2)
                .name("电流差报警")
                .expr("IX_II") // 最大电流 - 最小电流
                .batteryTypeId(1) // 三元电池
                .items(new ArrayList<>())
                .build();

        // 添加规则区间项
        addRuleItem(currentDiffRule, new BigDecimal("3"), null, 0); // 差值 >= 3，预警级别 0
        addRuleItem(currentDiffRule, new BigDecimal("1"), new BigDecimal("3"), 1); // 1 <= 差值 < 3，预警级别 1
        addRuleItem(currentDiffRule, new BigDecimal("0.2"), new BigDecimal("1"), 2); // 0.2 <= 差值 < 1，预警级别 2
    }

    private void addRuleItem(WarnRule rule, BigDecimal minVal, BigDecimal maxVal, Integer warnLevel) {
        WarnRuleItem item = WarnRuleItem.builder()
                .id((long) (rule.getItems().size() + 1))
                .minVal(minVal)
                .maxVal(maxVal)
                .warnLevel(warnLevel)
                .build();

        rule.addItem(item);
    }

    @Test
    void evaluateRule_VoltageDiff_Level0() {
        // Given - 电压差 = 6.0
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("8.0"));
        signalData.put("Mi", new BigDecimal("2.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(0, warnLevel);
    }

    @Test
    void evaluateRule_VoltageDiff_Level1() {
        // Given - 电压差 = 4.0
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("7.0"));
        signalData.put("Mi", new BigDecimal("3.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(1, warnLevel);
    }

    @Test
    void evaluateRule_VoltageDiff_Level2() {
        // Given - 电压差 = 2.0
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("5.0"));
        signalData.put("Mi", new BigDecimal("3.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(2, warnLevel);
    }

    @Test
    void evaluateRule_VoltageDiff_Level3() {
        // Given - 电压差 = 0.8
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("4.0"));
        signalData.put("Mi", new BigDecimal("3.2"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(3, warnLevel);
    }

    @Test
    void evaluateRule_VoltageDiff_Level4() {
        // Given - 电压差 = 0.5
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("4.0"));
        signalData.put("Mi", new BigDecimal("3.5"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(4, warnLevel);
    }

    @Test
    void evaluateRule_VoltageDiff_NoWarning() {
        // Given - 电压差 = 0.1
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("4.0"));
        signalData.put("Mi", new BigDecimal("3.9"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(voltageDiffRule, signalData);

        // Then
        assertNull(warnLevel);
    }

    @Test
    void evaluateRule_CurrentDiff_Level0() {
        // Given - 电流差 = 4.0
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Ix", new BigDecimal("10.0"));
        signalData.put("Ii", new BigDecimal("6.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(currentDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(0, warnLevel);
    }

    @Test
    void evaluateRule_CurrentDiff_Level1() {
        // Given - 电流差 = 2.0
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Ix", new BigDecimal("8.0"));
        signalData.put("Ii", new BigDecimal("6.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(currentDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(1, warnLevel);
    }

    @Test
    void evaluateRule_CurrentDiff_Level2() {
        // Given - 电流差 = 0.5
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Ix", new BigDecimal("6.5"));
        signalData.put("Ii", new BigDecimal("6.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(currentDiffRule, signalData);

        // Then
        assertNotNull(warnLevel);
        assertEquals(2, warnLevel);
    }

    @Test
    void evaluateRule_CurrentDiff_NoWarning() {
        // Given - 电流差 = 0.1
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Ix", new BigDecimal("6.1"));
        signalData.put("Ii", new BigDecimal("6.0"));

        // When
        Integer warnLevel = ruleEngine.evaluateRule(currentDiffRule, signalData);

        // Then
        assertNull(warnLevel);
    }

    @Test
    void parseSignalJson_Success() {
        // Given
        String signalJson = "{\"Mx\":12.0,\"Mi\":0.6,\"Ix\":10.0,\"Ii\":9.8}";

        // When
        Map<String, BigDecimal> result = ruleEngine.parseSignalJson(signalJson);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals(new BigDecimal("12.0"), result.get("Mx"));
        assertEquals(new BigDecimal("0.6"), result.get("Mi"));
        assertEquals(new BigDecimal("10.0"), result.get("Ix"));
        assertEquals(new BigDecimal("9.8"), result.get("Ii"));
    }

    @Test
    void evaluateRules_MultipleRules() {
        // Given - 同时触发电压差和电流差规则
        Map<String, BigDecimal> signalData = new HashMap<>();
        signalData.put("Mx", new BigDecimal("8.0")); // 电压差 = 5.0，预警级别 0
        signalData.put("Mi", new BigDecimal("3.0"));
        signalData.put("Ix", new BigDecimal("8.0")); // 电流差 = 2.0，预警级别 1
        signalData.put("Ii", new BigDecimal("6.0"));

        // When
        Map<Long, Integer> warningResults = ruleEngine.evaluateRules(
                java.util.Arrays.asList(voltageDiffRule, currentDiffRule),
                signalData);

        // Then
        assertNotNull(warningResults);
        assertEquals(2, warningResults.size());
        assertEquals(0, warningResults.get(voltageDiffRule.getId()));
        assertEquals(1, warningResults.get(currentDiffRule.getId()));
    }
}