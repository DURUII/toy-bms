package com.mi.bms.signal.interfaces.rest;

import com.mi.bms.shared.web.ApiResponse;
import com.mi.bms.signal.application.SignalService;
import com.mi.bms.signal.interfaces.rest.dto.SignalRequest;
import com.mi.bms.signal.interfaces.rest.dto.SignalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "信号管理")
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;

    @Operation(summary = "上报信号数据")
    @PostMapping
    public ApiResponse<List<SignalResponse>> reportSignals(
            @Parameter(description = "信号数据列表") @Valid @RequestBody List<SignalRequest> requests) {
        return ApiResponse.success(signalService.reportSignals(requests));
    }

    @Operation(summary = "查询信号数据")
    @GetMapping
    public ApiResponse<List<SignalResponse>> getSignals(
            @Parameter(description = "车辆ID（可选）") @RequestParam(required = false) Integer carId,
            @Parameter(description = "起始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (carId != null) {
            return ApiResponse.success(signalService.getSignalsByCarId(carId, from, to));
        }
        return ApiResponse.success(signalService.getAllSignals(from, to));
    }
}