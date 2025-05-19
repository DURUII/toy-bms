package com.mi.bms.vehicle.application.impl;

import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.application.VehicleService;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.VehicleRepository;
import com.mi.bms.vehicle.domain.service.VehicleDomainService;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDomainService vehicleDomainService;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        // Create vehicle status value object
        Vehicle.VehicleStatus status = Vehicle.VehicleStatus.create(
                request.getMileageKm(),
                request.getHealthPct());

        // Use domain service to create vehicle
        Vehicle vehicle = vehicleDomainService.createVehicle(
                request.getCarId(),
                request.getBatteryTypeCode(),
                status);

        // Save to database
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {}", savedVehicle.getVid());

        return buildVehicleResponse(savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(String vid) {
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "vid", vid));
        return buildVehicleResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByCarId(Integer carId) {
        Vehicle vehicle = vehicleDomainService.getVehicleByCarId(carId);
        return buildVehicleResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::buildVehicleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(String vid, VehicleRequest request) {
        // Find vehicle
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "vid", vid));

        // Get battery type
        BatteryType batteryType = vehicleDomainService.getBatteryTypeByCode(request.getBatteryTypeCode());

        // Update vehicle status
        Vehicle.VehicleStatus newStatus = Vehicle.VehicleStatus.create(
                request.getMileageKm(),
                request.getHealthPct());
        vehicle.updateStatus(newStatus);

        // Save changes
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: {}", updatedVehicle.getVid());

        return buildVehicleResponse(updatedVehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(String vid) {
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "vid", vid));

        vehicle.markAsDeleted();
        vehicleRepository.save(vehicle);
        log.info("Vehicle deleted: {}", vid);
    }

    @Override
    public Vehicle findVehicleEntity(Integer carId) {
        return vehicleDomainService.getVehicleByCarId(carId);
    }

    // Helper method for DTO conversion
    private VehicleResponse buildVehicleResponse(Vehicle vehicle) {
        BatteryType batteryType = vehicleDomainService.getBatteryTypeByCode(
                vehicle.getBatteryTypeId().toString());

        return VehicleResponse.builder()
                .vid(vehicle.getVid())
                .carId(vehicle.getCarId())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .mileageKm(vehicle.getStatus().getMileageKm())
                .healthPct(vehicle.getStatus().getHealthPct())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}