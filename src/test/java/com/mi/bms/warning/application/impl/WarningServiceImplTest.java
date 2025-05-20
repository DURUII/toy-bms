package com.mi.bms.warning.application.impl;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.service.RuleEngine;
import com.mi.bms.signal.domain.model.Signal;
import com.mi.bms.signal.domain.repository.SignalRepository;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.warning.domain.model.Warning;
import com.mi.bms.warning.domain.repository.WarningRepository;
import com.mi.bms.warning.infrastructure.cache.WarningCache;
import com.mi.bms.warning.interfaces.rest.dto.WarningResponse;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarningServiceImplTest {

    @Mock
    private SignalRepository signalRepository;
    @Mock
    private WarningRepository warningRepository;
    @Mock
    private BatteryTypeRepository batteryTypeRepository;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private WarningCache warningCache;

    @Captor
    private ArgumentCaptor<Warning> warningCaptor;

    private WarningServiceImpl warningService;

    @BeforeEach
    void setUp() {
        warningService = new WarningServiceImpl(
                signalRepository,
                warningRepository,
                batteryTypeRepository,
                ruleEngine,
                warningCache);
    }

    @Test
    void generateWarning_ShouldCreateWarningWhenRuleTriggered() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Integer ruleNo = 3;
        String ruleName = "Test Rule";
        Integer warnLevel = 2;

        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", new BigDecimal("3.5"));
        Signal signal = Signal.create(carId, batteryTypeId, signalValues);
        Signal savedSignal = Signal.create(carId, batteryTypeId, signalValues);
        Signal processedSignal = Signal.create(carId, batteryTypeId, signalValues);
        processedSignal.markAsProcessed();

        // Mock the save behavior to return different signals for each save
        when(signalRepository.save(any(Signal.class))).thenReturn(savedSignal, processedSignal);

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(batteryTypeId);

        WarnRule rule = mock(WarnRule.class);
        when(rule.getRuleNo()).thenReturn(ruleNo);
        when(rule.getName()).thenReturn(ruleName);

        WarnRule.RuleCondition condition = mock(WarnRule.RuleCondition.class);
        when(condition.getWarnLevel()).thenReturn(warnLevel);

        when(signalRepository.findById(any())).thenReturn(Optional.of(savedSignal));
        when(batteryTypeRepository.findById(batteryTypeId)).thenReturn(Optional.of(batteryType));
        when(ruleEngine.getRulesForBatteryType(batteryTypeId)).thenReturn(List.of(rule));
        when(ruleEngine.evaluateSignal(ruleNo, batteryTypeId, savedSignal.getValues().getVoltageDiff()))
                .thenReturn(Optional.of(condition));

        Warning savedWarning = Warning.create(carId, batteryTypeId, ruleNo, ruleName, warnLevel,
                savedSignal.getSignalData());
        when(warningRepository.save(any(Warning.class))).thenReturn(savedWarning);

        // When
        warningService.generateWarning(1L);

        // Then
        verify(warningRepository).save(warningCaptor.capture());
        verify(warningCache).invalidate(carId, batteryTypeId);
        verify(signalRepository).save(any(Signal.class));

        Warning capturedWarning = warningCaptor.getValue();
        assertEquals(carId, capturedWarning.getCarId());
        assertEquals(batteryTypeId, capturedWarning.getBatteryTypeId());
        assertEquals(ruleNo, capturedWarning.getRuleNo());
        assertEquals(ruleName, capturedWarning.getRuleName());
        assertEquals(warnLevel, capturedWarning.getWarnLevel());
        assertEquals(savedSignal.getSignalData(), capturedWarning.getSignalData());
        assertTrue(processedSignal.isProcessed());
    }

    @Test
    void generateWarning_ShouldNotCreateWarningWhenNoRuleTriggered() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;

        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", new BigDecimal("3.5"));
        Signal signal = Signal.create(carId, batteryTypeId, signalValues);
        Signal savedSignal = Signal.create(carId, batteryTypeId, signalValues);
        Signal processedSignal = Signal.create(carId, batteryTypeId, signalValues);
        processedSignal.markAsProcessed();

        // Mock the save behavior to return different signals for each save
        when(signalRepository.save(any(Signal.class))).thenReturn(savedSignal, processedSignal);

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(batteryTypeId);

        WarnRule rule = mock(WarnRule.class);
        when(rule.getRuleNo()).thenReturn(3);

        when(signalRepository.findById(any())).thenReturn(Optional.of(savedSignal));
        when(batteryTypeRepository.findById(batteryTypeId)).thenReturn(Optional.of(batteryType));
        when(ruleEngine.getRulesForBatteryType(batteryTypeId)).thenReturn(List.of(rule));
        when(ruleEngine.evaluateSignal(any(), any(), any())).thenReturn(Optional.empty());

        // When
        warningService.generateWarning(1L);

        // Then
        verify(warningRepository, never()).save(any());
        verify(warningCache, never()).invalidate(any(), any());
        verify(signalRepository).save(any(Signal.class));
        assertTrue(processedSignal.isProcessed());
    }

    @Test
    void getWarningsByCarId_ShouldReturnCachedWarnings() {
        // Given
        Integer carId = 1;
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(2);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Warning warning = Warning.create(carId, 2, 3, "Test Rule", 2, "{}");
        List<Warning> warnings = List.of(warning);

        when(warningCache.getByCarId(carId, from, to)).thenReturn(warnings);
        when(batteryTypeRepository.findAllById(List.of(2))).thenReturn(List.of(batteryType));

        // When
        List<WarningResponse> responses = warningService.getWarningsByCarId(carId, from, to);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        WarningResponse response = responses.get(0);
        assertEquals(carId, response.getCarId());
        assertEquals(2, response.getBatteryTypeId());
        assertEquals("BT001", response.getBatteryTypeCode());
        assertEquals("Test Battery", response.getBatteryTypeName());
        assertEquals(3, response.getRuleNo());
        assertEquals("Test Rule", response.getRuleName());
        assertEquals(2, response.getWarnLevel());

        verify(warningRepository, never()).findByCarIdAndTimeRange(any(), any(), any());
    }

    @Test
    void getWarningsByCarId_ShouldFetchFromDatabaseWhenCacheMiss() {
        // Given
        Integer carId = 1;
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(2);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Warning warning = Warning.create(carId, 2, 3, "Test Rule", 2, "{}");
        List<Warning> warnings = List.of(warning);

        when(warningCache.getByCarId(carId, from, to)).thenReturn(null);
        when(warningRepository.findByCarIdAndTimeRange(carId, from, to)).thenReturn(warnings);
        when(batteryTypeRepository.findAllById(List.of(2))).thenReturn(List.of(batteryType));

        // When
        List<WarningResponse> responses = warningService.getWarningsByCarId(carId, from, to);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        WarningResponse response = responses.get(0);
        assertEquals(carId, response.getCarId());
        assertEquals(2, response.getBatteryTypeId());
        assertEquals("BT001", response.getBatteryTypeCode());
        assertEquals("Test Battery", response.getBatteryTypeName());

        verify(warningCache).putByCarId(carId, from, to, warnings);
    }

    @Test
    void getWarningsByBatteryType_ShouldReturnWarnings() {
        // Given
        Integer batteryTypeId = 2;
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(batteryTypeId);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Warning warning1 = Warning.create(1, batteryTypeId, 3, "Test Rule 1", 2, "{}");
        Warning warning2 = Warning.create(2, batteryTypeId, 4, "Test Rule 2", 3, "{}");
        List<Warning> warnings = Arrays.asList(warning1, warning2);

        when(warningRepository.findByBatteryTypeAndTimeRange(batteryTypeId, from, to))
                .thenReturn(warnings);
        when(batteryTypeRepository.findAllById(List.of(batteryTypeId)))
                .thenReturn(List.of(batteryType));

        // When
        List<WarningResponse> responses = warningService.getWarningsByBatteryType(batteryTypeId, from, to);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());

        WarningResponse response1 = responses.get(0);
        assertEquals(1, response1.getCarId());
        assertEquals(batteryTypeId, response1.getBatteryTypeId());
        assertEquals("BT001", response1.getBatteryTypeCode());
        assertEquals("Test Battery", response1.getBatteryTypeName());
        assertEquals(3, response1.getRuleNo());
        assertEquals("Test Rule 1", response1.getRuleName());
        assertEquals(2, response1.getWarnLevel());

        WarningResponse response2 = responses.get(1);
        assertEquals(2, response2.getCarId());
        assertEquals(batteryTypeId, response2.getBatteryTypeId());
        assertEquals("BT001", response2.getBatteryTypeCode());
        assertEquals("Test Battery", response2.getBatteryTypeName());
        assertEquals(4, response2.getRuleNo());
        assertEquals("Test Rule 2", response2.getRuleName());
        assertEquals(3, response2.getWarnLevel());
    }

    @Test
    void getAllWarnings_ShouldReturnAllWarnings() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(2);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Warning warning1 = Warning.create(1, 2, 3, "Test Rule 1", 2, "{}");
        Warning warning2 = Warning.create(2, 2, 4, "Test Rule 2", 3, "{}");
        List<Warning> warnings = Arrays.asList(warning1, warning2);

        when(warningRepository.findByTimeRange(from, to)).thenReturn(warnings);
        when(batteryTypeRepository.findAllById(List.of(2))).thenReturn(List.of(batteryType));

        // When
        List<WarningResponse> responses = warningService.getAllWarnings(from, to);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());

        WarningResponse response1 = responses.get(0);
        assertEquals(1, response1.getCarId());
        assertEquals(2, response1.getBatteryTypeId());
        assertEquals("BT001", response1.getBatteryTypeCode());
        assertEquals("Test Battery", response1.getBatteryTypeName());
        assertEquals(3, response1.getRuleNo());
        assertEquals("Test Rule 1", response1.getRuleName());
        assertEquals(2, response1.getWarnLevel());

        WarningResponse response2 = responses.get(1);
        assertEquals(2, response2.getCarId());
        assertEquals(2, response2.getBatteryTypeId());
        assertEquals("BT001", response2.getBatteryTypeCode());
        assertEquals("Test Battery", response2.getBatteryTypeName());
        assertEquals(4, response2.getRuleNo());
        assertEquals("Test Rule 2", response2.getRuleName());
        assertEquals(3, response2.getWarnLevel());
    }
}