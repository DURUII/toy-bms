package com.mi.bms.warning.interfaces.rest;

import com.mi.bms.shared.web.ApiResponse;
import com.mi.bms.warning.application.WarningService;
import com.mi.bms.warning.interfaces.rest.dto.WarningResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "警告管理")
@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningService;

    @Operation(summary = "查询警告信息", description = "可以根据车辆ID、电池类型ID、时间范围等条件进行查询")
    @GetMapping
    public ApiResponse<List<WarningResponse>> getWarnings(
            @Parameter(description = "车辆ID（可选）") @RequestParam(required = false) Integer carId,
            @Parameter(description = "电池类型ID（可选）") @RequestParam(required = false) Integer batteryTypeId,
            @Parameter(description = "起始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (carId != null && batteryTypeId != null) {
            return ApiResponse.success(warningService.getWarningsByCarIdAndBatteryType(
                    carId, batteryTypeId, from, to));
        }

        if (carId != null) {
            return ApiResponse.success(warningService.getWarningsByCarId(carId, from, to));
        }

        if (batteryTypeId != null) {
            return ApiResponse.success(warningService.getWarningsByBatteryType(batteryTypeId, from, to));
        }

        return ApiResponse.success(warningService.getAllWarnings(from, to));
    }
}