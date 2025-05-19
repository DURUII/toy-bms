package com.mi.bms.warning.interfaces.rest;

import com.mi.bms.warning.application.WarningService;
import com.mi.bms.warning.interfaces.rest.dto.WarningResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningService;

    @GetMapping
    public ResponseEntity<List<WarningResponse>> getWarnings(
            @RequestParam(required = false) Integer carId,
            @RequestParam(required = false) Integer batteryTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (carId != null && batteryTypeId != null) {
            return ResponseEntity.ok(warningService.getWarningsByCarIdAndBatteryType(
                    carId, batteryTypeId, from, to));
        }

        if (carId != null) {
            return ResponseEntity.ok(warningService.getWarningsByCarId(carId, from, to));
        }

        if (batteryTypeId != null) {
            return ResponseEntity.ok(warningService.getWarningsByBatteryType(batteryTypeId, from, to));
        }

        return ResponseEntity.ok(warningService.getAllWarnings(from, to));
    }
}