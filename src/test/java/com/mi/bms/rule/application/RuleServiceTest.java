package com.mi.bms.rule.application;

import com.mi.bms.rule.application.impl.RuleServiceImpl;
import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRuleItem;
import com.mi.bms.rule.domain.repository.WarnRuleRepository;
import com.mi.bms.rule.interfaces.rest.dto.RuleItemRequest;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleRequest;
import com.mi.bms.rule.interfaces.rest.dto.WarnRuleResponse;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RuleServiceTest {

    @Mock
    private WarnRuleRepository ruleRepository;

    @Mock
    private BatteryTypeRepository batteryTypeRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RuleServiceImpl ruleServiceImpl;

    private RuleService ruleService;

    private BatteryType ternaryBatteryType;
    private WarnRule warnRule;
    private WarnRuleRequest warnRuleRequest;

    @BeforeEach
    void setUp() {
        // Cast the implementation to the interface to avoid ambiguity
        ruleService = ruleServiceImpl;

        // Setup Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 初始化测试数据
        ternaryBatteryType = BatteryType.builder()
                .id(1)
                .code("TERNARY")
                .name("三元电池")
                .build();

        // 创建预警规则
        warnRule = WarnRule.builder()
                .id(1L)
                .ruleNo(1)
                .name("电压差报警")
                .expr("MX_MI")
                .batteryTypeId(1)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 添加规则区间项
        WarnRuleItem item1 = WarnRuleItem.builder()
                .id(1L)
                .minVal(new BigDecimal("5"))
                .maxVal(null)
                .warnLevel(0)
                .build();
        item1.setRule(warnRule);
        warnRule.getItems().add(item1);

        WarnRuleItem item2 = WarnRuleItem.builder()
                .id(2L)
                .minVal(new BigDecimal("3"))
                .maxVal(new BigDecimal("5"))
                .warnLevel(1)
                .build();
        item2.setRule(warnRule);
        warnRule.getItems().add(item2);

        // 创建预警规则请求
        List<RuleItemRequest> itemRequests = Arrays.asList(
                RuleItemRequest.builder().minVal(new BigDecimal("5")).maxVal(null).warnLevel(0).build(),
                RuleItemRequest.builder().minVal(new BigDecimal("3")).maxVal(new BigDecimal("5")).warnLevel(1).build());

        warnRuleRequest = WarnRuleRequest.builder()
                .ruleNo(1)
                .name("电压差报警")
                .expr("MX_MI")
                .batteryTypeCode("TERNARY")
                .items(itemRequests)
                .build();
    }

    @Test
    void createRule_Success() {
        // Given
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.of(ternaryBatteryType));
        when(ruleRepository.save(any(WarnRule.class))).thenReturn(warnRule);

        // When
        WarnRuleResponse response = ruleService.createRule(warnRuleRequest);

        // Then
        assertNotNull(response);
        assertEquals(warnRule.getId(), response.getId());
        assertEquals(warnRule.getRuleNo(), response.getRuleNo());
        assertEquals(warnRule.getName(), response.getName());
        assertEquals(warnRule.getExpr(), response.getExpr());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());
        assertEquals(ternaryBatteryType.getName(), response.getBatteryTypeName());
        assertEquals(2, response.getItems().size());

        verify(batteryTypeRepository).findByCode(warnRuleRequest.getBatteryTypeCode());
        verify(ruleRepository).save(any(WarnRule.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void createRule_InvalidBatteryType_ThrowsException() {
        // Given
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> ruleService.createRule(warnRuleRequest));

        assertTrue(exception.getMessage().contains("BatteryType not found"));

        verify(batteryTypeRepository).findByCode(warnRuleRequest.getBatteryTypeCode());
        verify(ruleRepository, never()).save(any(WarnRule.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void getRuleById_Success() {
        // Given
        when(ruleRepository.findByIdWithItems(anyLong())).thenReturn(Optional.of(warnRule));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When
        WarnRuleResponse response = ruleService.getRuleById(1L);

        // Then
        assertNotNull(response);
        assertEquals(warnRule.getId(), response.getId());
        assertEquals(warnRule.getRuleNo(), response.getRuleNo());
        assertEquals(warnRule.getName(), response.getName());
        assertEquals(warnRule.getExpr(), response.getExpr());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());
        assertEquals(ternaryBatteryType.getName(), response.getBatteryTypeName());
        assertEquals(2, response.getItems().size());

        verify(ruleRepository).findByIdWithItems(1L);
        verify(batteryTypeRepository).findById(warnRule.getBatteryTypeId());
    }

    @Test
    void getRuleById_NotFound_ThrowsException() {
        // Given
        when(ruleRepository.findByIdWithItems(anyLong())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> ruleService.getRuleById(1L));

        assertTrue(exception.getMessage().contains("Rule not found"));

        verify(ruleRepository).findByIdWithItems(1L);
        verify(batteryTypeRepository, never()).findById(anyInt());
    }

    @Test
    void findRules_ByRuleNoAndBatteryType_Success() {
        // Given
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.of(ternaryBatteryType));
        when(ruleRepository.findByRuleNoAndBatteryTypeIdWithItems(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(warnRule));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When - explicitly use the service interface method
        List<WarnRuleResponse> responses = ruleService.findRules(1, "TERNARY");

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        WarnRuleResponse response = responses.get(0);
        assertEquals(warnRule.getId(), response.getId());
        assertEquals(warnRule.getRuleNo(), response.getRuleNo());
        assertEquals(warnRule.getName(), response.getName());

        verify(batteryTypeRepository).findByCode("TERNARY");
        verify(ruleRepository).findByRuleNoAndBatteryTypeIdWithItems(1, ternaryBatteryType.getId());
        verify(batteryTypeRepository).findById(warnRule.getBatteryTypeId());
    }

    @Test
    void findRules_ByRuleNo_Success() {
        // Given
        when(ruleRepository.findByRuleNo(anyInt())).thenReturn(Arrays.asList(warnRule));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When - pass Integer for ruleNo and null for batteryTypeCode
        List<WarnRuleResponse> responses = ((RuleService) ruleServiceImpl).findRules(1, null);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(ruleRepository).findByRuleNo(1);
        verify(batteryTypeRepository).findById(warnRule.getBatteryTypeId());
    }

    @Test
    void findRules_ByBatteryType_Success() {
        // Given
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.of(ternaryBatteryType));
        when(ruleRepository.findByBatteryTypeId(anyInt())).thenReturn(Arrays.asList(warnRule));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When - pass null for ruleNo and String for batteryTypeCode
        List<WarnRuleResponse> responses = ((RuleService) ruleServiceImpl).findRules(null, "TERNARY");

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(batteryTypeRepository).findByCode("TERNARY");
        verify(ruleRepository).findByBatteryTypeId(ternaryBatteryType.getId());
        verify(batteryTypeRepository).findById(warnRule.getBatteryTypeId());
    }

    @Test
    void findRules_All_Success() {
        // Given
        when(ruleRepository.findAll()).thenReturn(Arrays.asList(warnRule));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When - both params are null, explicitly cast to RuleService
        List<WarnRuleResponse> responses = ((RuleService) ruleServiceImpl).findRules(null, null);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        verify(ruleRepository).findAll();
        verify(batteryTypeRepository).findById(warnRule.getBatteryTypeId());
    }
}