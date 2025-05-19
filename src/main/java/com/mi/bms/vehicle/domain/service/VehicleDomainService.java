package com.mi.bms.vehicle.domain.service;

import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleDomainService {

    private final VehicleRepository vehicleRepository;
    private final BatteryTypeRepository batteryTypeRepository;

    @Transactional
    public Vehicle createVehicle(Integer carId, String batteryTypeCode, Vehicle.VehicleStatus status) {
        // Check if carId already exists
        if (vehicleRepository.existsByCarId(carId)) {
            throw new BusinessException("ALREADY_EXISTS", "车架号已存在");
        }

        // Get battery type
        BatteryType batteryType = getBatteryTypeByCode(batteryTypeCode);

        // Generate VID
        String vid = generateVid();

        // Create vehicle aggregate
        return Vehicle.create(vid, carId, batteryType.getId(), status);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleByCarId(Integer carId) {
        return vehicleRepository.findByCarId(carId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Vehicle not found with carId: " + carId));
    }

    @Transactional(readOnly = true)
    public BatteryType getBatteryTypeByCode(String code) {
        return batteryTypeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "BatteryType not found with code: " + code));
    }

    private String generateVid() {
        return UUID.randomUUID().toString().substring(0, 16);
    }
}