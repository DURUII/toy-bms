package com.mi.bms.signal.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.signal.domain.model.Signal;
import com.mi.bms.signal.domain.repository.SignalRepository;
import com.mi.bms.signal.infrastructure.mq.SignalProducer;
import com.mi.bms.signal.interfaces.rest.dto.SignalRequest;
import com.mi.bms.signal.interfaces.rest.dto.SignalResponse;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.domain.service.VehicleDomainService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignalServiceImplTest {

    @Mock
    private SignalRepository signalRepository;
    @Mock
    private VehicleDomainService vehicleDomainService;
    @Mock
    private BatteryTypeRepository batteryTypeRepository;
    @Mock
    private SignalProducer signalProducer;
    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<Signal> signalCaptor;

    private SignalServiceImpl signalService;
    private ObjectMapper realObjectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        signalService = new SignalServiceImpl(
                signalRepository,
                vehicleDomainService,
                batteryTypeRepository,
                signalProducer,
                objectMapper);
    }

    @Test
    void reportSignals_ShouldSaveAndSendSignals() throws Exception {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        String signalData = "{\"Mx\":3.8,\"Mi\":3.5,\"Ix\":100,\"Ii\":80}";

        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getBatteryTypeId()).thenReturn(batteryTypeId);

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(batteryTypeId);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        SignalRequest request = new SignalRequest();
        request.setCarId(carId);
        request.setSignal(signalData);

        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", new BigDecimal("3.5"));
        signalValues.put("Ix", new BigDecimal("100"));
        signalValues.put("Ii", new BigDecimal("80"));

        when(vehicleDomainService.getVehicleByCarId(carId)).thenReturn(vehicle);
        when(batteryTypeRepository.findById(batteryTypeId)).thenReturn(Optional.of(batteryType));
        when(objectMapper.readValue(eq(signalData), any(Class.class))).thenReturn(signalValues);

        Signal savedSignal = Signal.create(carId, batteryTypeId, signalValues);
        // Initialize createdAt and updatedAt fields for testing
        setTimestamps(savedSignal);
        when(signalRepository.save(any(Signal.class))).thenReturn(savedSignal);

        // When
        List<SignalResponse> responses = signalService.reportSignals(List.of(request));

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        SignalResponse response = responses.get(0);
        assertEquals(carId, response.getCarId());
        assertEquals(batteryTypeId, response.getBatteryTypeId());
        assertEquals("BT001", response.getBatteryTypeCode());
        assertEquals("Test Battery", response.getBatteryTypeName());

        // Compare with real JSON contents
        if (response.getSignalData() != null) {
            Map<String, Object> expectedMap = realObjectMapper.readValue(signalData, Map.class);
            Map<String, Object> actualMap = realObjectMapper.readValue(response.getSignalData(), Map.class);
            assertEquals(expectedMap, actualMap);
        }

        assertFalse(response.isProcessed());

        verify(signalRepository).save(signalCaptor.capture());
        verify(signalProducer).sendSignal(savedSignal);

        Signal capturedSignal = signalCaptor.getValue();
        assertEquals(carId, capturedSignal.getCarId());
        assertEquals(batteryTypeId, capturedSignal.getBatteryTypeId());
    }

    @Test
    void reportSignals_ShouldHandleInvalidSignalData() throws Exception {
        // Given
        Integer carId = 1;
        String invalidSignalData = "invalid json";

        SignalRequest request = new SignalRequest();
        request.setCarId(carId);
        request.setSignal(invalidSignalData);

        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getBatteryTypeId()).thenReturn(2);

        when(vehicleDomainService.getVehicleByCarId(carId)).thenReturn(vehicle);
        when(objectMapper.readValue(eq(invalidSignalData), any(Class.class)))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> signalService.reportSignals(List.of(request)));
        verify(signalRepository, never()).save(any());
        verify(signalProducer, never()).sendSignal(any());
    }

    @Test
    void getSignalsByCarId_ShouldReturnSignals() {
        // Given
        Integer carId = 1;
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(2);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", new BigDecimal("3.5"));

        Signal signal = Signal.create(carId, 2, signalValues);
        // Initialize createdAt and updatedAt fields for testing
        setTimestamps(signal);

        when(signalRepository.findByCarIdAndTimeRange(carId, from, to))
                .thenReturn(List.of(signal));
        when(batteryTypeRepository.findAllById(List.of(2)))
                .thenReturn(List.of(batteryType));

        // When
        List<SignalResponse> responses = signalService.getSignalsByCarId(carId, from, to);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());

        SignalResponse response = responses.get(0);
        assertEquals(carId, response.getCarId());
        assertEquals(2, response.getBatteryTypeId());
        assertEquals("BT001", response.getBatteryTypeCode());
        assertEquals("Test Battery", response.getBatteryTypeName());
    }

    @Test
    void getAllSignals_ShouldReturnAllSignals() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        BatteryType batteryType = mock(BatteryType.class);
        when(batteryType.getId()).thenReturn(2);
        when(batteryType.getCode()).thenReturn("BT001");
        when(batteryType.getName()).thenReturn("Test Battery");

        Map<String, BigDecimal> signalValues1 = new HashMap<>();
        signalValues1.put("Mx", new BigDecimal("3.8"));
        Map<String, BigDecimal> signalValues2 = new HashMap<>();
        signalValues2.put("Mx", new BigDecimal("3.9"));

        Signal signal1 = Signal.create(1, 2, signalValues1);
        // Initialize createdAt and updatedAt fields for testing
        setTimestamps(signal1);
        Signal signal2 = Signal.create(2, 2, signalValues2);
        // Initialize createdAt and updatedAt fields for testing
        setTimestamps(signal2);

        when(signalRepository.findByTimeRange(from, to))
                .thenReturn(Arrays.asList(signal1, signal2));
        when(batteryTypeRepository.findAllById(List.of(2)))
                .thenReturn(List.of(batteryType));

        // When
        List<SignalResponse> responses = signalService.getAllSignals(from, to);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());

        SignalResponse response1 = responses.get(0);
        assertEquals(1, response1.getCarId());
        assertEquals(2, response1.getBatteryTypeId());
        assertEquals("BT001", response1.getBatteryTypeCode());

        SignalResponse response2 = responses.get(1);
        assertEquals(2, response2.getCarId());
        assertEquals(2, response2.getBatteryTypeId());
        assertEquals("BT001", response2.getBatteryTypeCode());
    }

    /**
     * Helper method to set timestamps for testing
     */
    private void setTimestamps(Signal signal) {
        try {
            java.lang.reflect.Field createdAtField = Signal.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(signal, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = Signal.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(signal, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set timestamps for testing", e);
        }
    }
}