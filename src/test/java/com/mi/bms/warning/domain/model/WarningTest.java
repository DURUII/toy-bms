package com.mi.bms.warning.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class WarningTest {

    @Test
    void create_ShouldCreateWarningWithCorrectValues() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Integer ruleNo = 3;
        String ruleName = "Test Rule";
        Integer warnLevel = 2;
        String signalData = "{\"Mx\":3.8,\"Mi\":3.5}";

        // When
        Warning warning = Warning.create(carId, batteryTypeId, ruleNo, ruleName, warnLevel, signalData);
        // Simulate JPA lifecycle events
        warning.onCreate();

        // Then
        assertNotNull(warning);
        assertEquals(carId, warning.getCarId());
        assertEquals(batteryTypeId, warning.getBatteryTypeId());
        assertEquals(ruleNo, warning.getRuleNo());
        assertEquals(ruleName, warning.getRuleName());
        assertEquals(warnLevel, warning.getWarnLevel());
        assertEquals(signalData, warning.getSignalData());
        assertFalse(warning.isDelete());
        assertNotNull(warning.getCreatedAt());
        assertNotNull(warning.getUpdatedAt());
        assertEquals(warning.getCreatedAt(), warning.getUpdatedAt());
    }

    @Test
    void markAsDeleted_ShouldUpdateDeleteFlag() {
        // Given
        Integer carId = 1;
        Integer batteryTypeId = 2;
        Integer ruleNo = 3;
        String ruleName = "Test Rule";
        Integer warnLevel = 2;
        String signalData = "{\"Mx\":3.8,\"Mi\":3.5}";
        Warning warning = Warning.create(carId, batteryTypeId, ruleNo, ruleName, warnLevel, signalData);
        // Simulate JPA lifecycle events
        warning.onCreate();

        // Ensure there's a small delay between creation and update timestamps
        LocalDateTime beforeUpdate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // When
        warning.markAsDeleted();

        // Then
        assertTrue(warning.isDelete());
        assertNotNull(warning.getUpdatedAt());
        assertTrue(warning.getUpdatedAt().isAfter(beforeUpdate) ||
                warning.getUpdatedAt().equals(beforeUpdate));
    }
}