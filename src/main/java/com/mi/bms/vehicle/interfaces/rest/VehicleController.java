package com.mi.bms.vehicle.interfaces.rest;

import com.mi.bms.shared.web.ApiResponse;
import com.mi.bms.vehicle.application.VehicleService;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "车辆管理")
@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "创建车辆")
    @PostMapping
    public ApiResponse<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ApiResponse.success(vehicleService.createVehicle(request));
    }

    @Operation(summary = "根据VID查询车辆")
    @GetMapping("/{vid}")
    public ApiResponse<VehicleResponse> getVehicleById(@PathVariable String vid) {
        return ApiResponse.success(vehicleService.getVehicleById(vid));
    }

    @Operation(summary = "根据车架号查询车辆")
    @GetMapping("/carId/{carId}")
    public ApiResponse<VehicleResponse> getVehicleByCarId(@PathVariable Integer carId) {
        return ApiResponse.success(vehicleService.getVehicleByCarId(carId));
    }

    @Operation(summary = "查询所有车辆")
    @GetMapping
    public ApiResponse<List<VehicleResponse>> getAllVehicles() {
        return ApiResponse.success(vehicleService.getAllVehicles());
    }

    @Operation(summary = "更新车辆信息")
    @PutMapping("/{vid}")
    public ApiResponse<VehicleResponse> updateVehicle(
            @PathVariable String vid,
            @Valid @RequestBody VehicleRequest request) {
        return ApiResponse.success(vehicleService.updateVehicle(vid, request));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{vid}")
    public ApiResponse<Void> deleteVehicle(@PathVariable String vid) {
        vehicleService.deleteVehicle(vid);
        return ApiResponse.success();
    }
}