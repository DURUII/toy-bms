package com.mi.bms.rule.application;

import com.mi.bms.rule.application.impl.RuleServiceImpl;
import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRule.RuleCondition;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleConditionRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleServiceTest {

        @Mock
        private WarnRuleRepository ruleRepository;
        @Mock
        private BatteryTypeRepository batteryTypeRepository;

        @Captor
        private ArgumentCaptor<WarnRule> ruleCaptor;

        private RuleService ruleService;
        private BatteryType batteryType;

        @BeforeEach
        void setUp() {
                ruleService = new RuleServiceImpl(ruleRepository, batteryTypeRepository);

                // Setup battery type
                batteryType = mock(BatteryType.class);
                when(batteryType.getId()).thenReturn(1);
                when(batteryType.getCode()).thenReturn("TERNARY");
                when(batteryType.getName()).thenReturn("三元电池");
                when(batteryTypeRepository.findById(1)).thenReturn(Optional.of(batteryType));
        }

        @Test
        void createRule_ShouldCreateNewRule() {
                // Given
                when(ruleRepository.existsByRuleNo(1)).thenReturn(false);

                RuleRequest request = new RuleRequest();
                request.setRuleNo(1);
                request.setName("电压差报警");
                request.setExpr("MX_MI");
                request.setBatteryTypeId(1);

                List<RuleConditionRequest> conditions = new ArrayList<>();
                RuleConditionRequest condition1 = new RuleConditionRequest();
                condition1.setMinVal(new BigDecimal("5"));
                condition1.setMaxVal(null);
                condition1.setWarnLevel(0);
                conditions.add(condition1);

                RuleConditionRequest condition2 = new RuleConditionRequest();
                condition2.setMinVal(new BigDecimal("3"));
                condition2.setMaxVal(new BigDecimal("5"));
                condition2.setWarnLevel(1);
                conditions.add(condition2);

                request.setConditions(conditions);

                // Mock the save behavior to return the saved rule
                when(ruleRepository.save(any(WarnRule.class))).thenAnswer(invocation -> {
                        WarnRule rule = invocation.getArgument(0);
                        // Create a new rule with the same properties and conditions
                        WarnRule savedRule = WarnRule.create(rule.getRuleNo(), rule.getName(), rule.getExpr(),
                                        rule.getBatteryTypeId());
                        // Copy all conditions from the input rule
                        rule.getConditions().forEach(condition -> {
                                RuleCondition newCondition = RuleCondition.create(
                                                condition.getMinVal(),
                                                condition.getMaxVal(),
                                                condition.getWarnLevel());
                                savedRule.addCondition(newCondition);
                        });
                        return savedRule;
                });

                // When
                RuleResponse response = ruleService.createRule(request);

                // Then
                assertNotNull(response);
                assertEquals(1, response.getRuleNo());
                assertEquals("电压差报警", response.getName());
                assertEquals("MX_MI", response.getExpr());
                assertEquals("TERNARY", response.getBatteryTypeCode());
                assertEquals("三元电池", response.getBatteryTypeName());
                assertEquals(2, response.getConditions().size());

                verify(ruleRepository).save(ruleCaptor.capture());
                WarnRule savedRule = ruleCaptor.getValue();
                assertEquals(1, savedRule.getRuleNo());
                assertEquals("电压差报警", savedRule.getName());
                assertEquals("MX_MI", savedRule.getExpr());
                assertEquals(1, savedRule.getBatteryTypeId());
                assertEquals(2, savedRule.getConditions().size());
        }

        @Test
        void createRule_ShouldThrowExceptionWhenRuleNoExists() {
                // Given
                when(ruleRepository.existsByRuleNo(1)).thenReturn(true);

                RuleRequest request = new RuleRequest();
                request.setRuleNo(1);
                request.setName("电压差报警");
                request.setExpr("MX_MI");
                request.setBatteryTypeId(1);

                // When & Then
                assertThrows(RuntimeException.class, () -> ruleService.createRule(request));
                verify(ruleRepository, never()).save(any());
        }

        @Test
        void updateRule_ShouldUpdateExistingRule() {
                // Given
                WarnRule existingRule = WarnRule.create(1, "电压差报警", "MX_MI", 1);
                when(ruleRepository.findById(1L)).thenReturn(Optional.of(existingRule));
                when(ruleRepository.existsByRuleNo(1)).thenReturn(true);

                RuleRequest request = new RuleRequest();
                request.setRuleNo(1);
                request.setName("电压差报警(更新)");
                request.setExpr("MX_MI");
                request.setBatteryTypeId(1);

                List<RuleConditionRequest> conditions = new ArrayList<>();
                RuleConditionRequest condition1 = new RuleConditionRequest();
                condition1.setMinVal(new BigDecimal("5"));
                condition1.setMaxVal(null);
                condition1.setWarnLevel(0);
                conditions.add(condition1);

                RuleConditionRequest condition2 = new RuleConditionRequest();
                condition2.setMinVal(new BigDecimal("3"));
                condition2.setMaxVal(new BigDecimal("5"));
                condition2.setWarnLevel(1);
                conditions.add(condition2);

                request.setConditions(conditions);

                // Mock the save behavior to return the updated rule
                when(ruleRepository.save(any(WarnRule.class))).thenAnswer(invocation -> {
                        WarnRule rule = invocation.getArgument(0);
                        // Create a new rule with the same properties and conditions
                        WarnRule savedRule = WarnRule.create(rule.getRuleNo(), rule.getName(), rule.getExpr(),
                                        rule.getBatteryTypeId());
                        // Copy all conditions from the input rule
                        rule.getConditions().forEach(condition -> {
                                RuleCondition newCondition = RuleCondition.create(
                                                condition.getMinVal(),
                                                condition.getMaxVal(),
                                                condition.getWarnLevel());
                                savedRule.addCondition(newCondition);
                        });
                        return savedRule;
                });

                // When
                RuleResponse response = ruleService.updateRule(1L, request);

                // Then
                assertNotNull(response);
                assertEquals(1, response.getRuleNo());
                assertEquals("电压差报警(更新)", response.getName());
                assertEquals("MX_MI", response.getExpr());
                assertEquals("TERNARY", response.getBatteryTypeCode());
                assertEquals("三元电池", response.getBatteryTypeName());
                assertEquals(2, response.getConditions().size());

                verify(ruleRepository).save(ruleCaptor.capture());
                WarnRule updatedRule = ruleCaptor.getValue();
                assertEquals(1, updatedRule.getRuleNo());
                assertEquals("电压差报警(更新)", updatedRule.getName());
                assertEquals("MX_MI", updatedRule.getExpr());
                assertEquals(1, updatedRule.getBatteryTypeId());
                assertEquals(2, updatedRule.getConditions().size());
        }

        @Test
        void updateRule_ShouldThrowExceptionWhenRuleNotFound() {
                // Given
                when(ruleRepository.findById(1L)).thenReturn(Optional.empty());

                RuleRequest request = new RuleRequest();
                request.setRuleNo(1);
                request.setName("电压差报警");
                request.setExpr("MX_MI");
                request.setBatteryTypeId(1);

                // When & Then
                assertThrows(RuntimeException.class, () -> ruleService.updateRule(1L, request));
                verify(ruleRepository, never()).save(any());
        }

        @Test
        void deleteRule_ShouldMarkRuleAsDeleted() {
                // Given
                WarnRule rule = WarnRule.create(1, "电压差报警", "MX_MI", 1);
                when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

                // Mock the save behavior to return the deleted rule
                when(ruleRepository.save(any(WarnRule.class))).thenAnswer(invocation -> {
                        WarnRule savedRule = invocation.getArgument(0);
                        savedRule.markAsDeleted();
                        return savedRule;
                });

                // When
                ruleService.deleteRule(1L);

                // Then
                verify(ruleRepository).save(ruleCaptor.capture());
                WarnRule deletedRule = ruleCaptor.getValue();
                assertTrue(deletedRule.isDeleted());
        }

        @Test
        void deleteRule_ShouldThrowExceptionWhenRuleNotFound() {
                // Given
                when(ruleRepository.findById(1L)).thenReturn(Optional.empty());

                // When & Then
                assertThrows(RuntimeException.class, () -> ruleService.deleteRule(1L));
                verify(ruleRepository, never()).save(any());
        }

        @Test
        void getRuleById_ShouldReturnRule() {
                // Given
                WarnRule rule = WarnRule.create(1, "电压差报警", "MX_MI", 1);
                when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

                // When
                RuleResponse response = ruleService.getRuleById(1L);

                // Then
                assertNotNull(response);
                assertEquals(1, response.getRuleNo());
                assertEquals("电压差报警", response.getName());
                assertEquals("MX_MI", response.getExpr());
                assertEquals("TERNARY", response.getBatteryTypeCode());
                assertEquals("三元电池", response.getBatteryTypeName());
        }

        @Test
        void getRuleById_ShouldThrowExceptionWhenRuleNotFound() {
                // Given
                when(ruleRepository.findById(1L)).thenReturn(Optional.empty());

                // When & Then
                assertThrows(RuntimeException.class, () -> ruleService.getRuleById(1L));
        }

        @Test
        void getAllRules_ShouldReturnAllRules() {
                // Given
                WarnRule rule1 = WarnRule.create(1, "电压差报警", "MX_MI", 1);
                WarnRule rule2 = WarnRule.create(2, "电流差报警", "IX_II", 1);
                when(ruleRepository.findAll()).thenReturn(List.of(rule1, rule2));

                // When
                List<RuleResponse> responses = ruleService.getAllRules();

                // Then
                assertNotNull(responses);
                assertEquals(2, responses.size());

                RuleResponse response1 = responses.get(0);
                assertEquals(1, response1.getRuleNo());
                assertEquals("电压差报警", response1.getName());
                assertEquals("MX_MI", response1.getExpr());
                assertEquals("TERNARY", response1.getBatteryTypeCode());
                assertEquals("三元电池", response1.getBatteryTypeName());

                RuleResponse response2 = responses.get(1);
                assertEquals(2, response2.getRuleNo());
                assertEquals("电流差报警", response2.getName());
                assertEquals("IX_II", response2.getExpr());
                assertEquals("TERNARY", response2.getBatteryTypeCode());
                assertEquals("三元电池", response2.getBatteryTypeName());
        }
}