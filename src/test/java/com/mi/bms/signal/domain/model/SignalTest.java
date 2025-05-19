package com.mi.bms.signal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SignalTest {

    @Test
    void create_ShouldCreateSignalWithCorrectValues() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("Mx", new BigDecimal("3.8"));
        values.put("Mi", new BigDecimal("3.5"));

        // When
        Signal signal = Signal.create(carId, batteryTypeId, values);
        // Simulate JPA lifecycle events
        signal.onCreate();

        // Then
        assertNotNull(signal);
        assertEquals(carId, signal.getCarId());
        assertEquals(batteryTypeId, signal.getBatteryTypeId());
        assertNotNull(signal.getValues());
        assertEquals(new BigDecimal("3.8"), signal.getValues().getMaxVoltage());
        assertEquals(new BigDecimal("3.5"), signal.getValues().getMinVoltage());
        assertNotNull(signal.getValues().getVoltageDiff());
        assertEquals(new BigDecimal("0.3"), signal.getValues().getVoltageDiff());
        assertFalse(signal.isProcessed());
        assertFalse(signal.isDelete());
        assertNotNull(signal.getCreatedAt());
        assertNotNull(signal.getUpdatedAt());
        assertEquals(signal.getCreatedAt(), signal.getUpdatedAt());
    }

    @Test
    void markAsProcessed_ShouldUpdateProcessedFlag() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("Mx", new BigDecimal("3.8"));
        values.put("Mi", new BigDecimal("3.5"));
        Signal signal = Signal.create(carId, batteryTypeId, values);
        // Simulate JPA lifecycle events
        signal.onCreate();

        // Ensure there's a small delay between creation and update timestamps
        LocalDateTime beforeUpdate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // When
        signal.markAsProcessed();

        // Then
        assertTrue(signal.isProcessed());
        assertNotNull(signal.getUpdatedAt());
        assertTrue(signal.getUpdatedAt().isAfter(beforeUpdate) ||
                signal.getUpdatedAt().equals(beforeUpdate));
    }

    @Test
    void markAsDeleted_ShouldUpdateDeleteFlag() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("Mx", new BigDecimal("3.8"));
        values.put("Mi", new BigDecimal("3.5"));
        Signal signal = Signal.create(carId, batteryTypeId, values);
        // Simulate JPA lifecycle events
        signal.onCreate();

        // Ensure there's a small delay between creation and update timestamps
        LocalDateTime beforeUpdate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // When
        signal.markAsDeleted();

        // Then
        assertTrue(signal.isDelete());
        assertNotNull(signal.getUpdatedAt());
        assertTrue(signal.getUpdatedAt().isAfter(beforeUpdate) ||
                signal.getUpdatedAt().equals(beforeUpdate));
    }

    @Test
    void signalValues_ShouldHandleNullValues() {
        // Given
        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", null);
        signalValues.put("Ix", new BigDecimal("100"));
        signalValues.put("Ii", null);

        // When
        Signal signal = Signal.create(1, 2, signalValues);
        Signal.SignalValues values = signal.getValues();

        // Then
        assertNotNull(values);
        assertEquals(new BigDecimal("3.8"), values.getMaxVoltage());
        assertNull(values.getMinVoltage());
        assertEquals(new BigDecimal("100"), values.getMaxCurrent());
        assertNull(values.getMinCurrent());
        assertNull(values.getVoltageDiff());
        assertNull(values.getCurrentDiff());
    }

    @Test
    void signalValues_ShouldConvertToJson() throws Exception {
        // Given
        Map<String, BigDecimal> signalValues = new HashMap<>();
        signalValues.put("Mx", new BigDecimal("3.8"));
        signalValues.put("Mi", new BigDecimal("3.5"));
        signalValues.put("Ix", new BigDecimal("100"));
        signalValues.put("Ii", new BigDecimal("80"));

        // When
        Signal signal = Signal.create(1, 2, signalValues);
        String json = signal.getValues().toJson();

        // Then
        assertTrue(json.contains("\"Mx\":3.8"));
        assertTrue(json.contains("\"Mi\":3.5"));
        assertTrue(json.contains("\"Ix\":100"));
        assertTrue(json.contains("\"Ii\":80"));
    }
}