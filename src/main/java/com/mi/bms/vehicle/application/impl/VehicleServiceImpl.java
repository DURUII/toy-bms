package com.mi.bms.vehicle.application.impl;

import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.application.VehicleService;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.domain.repository.VehicleRepository;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BatteryTypeRepository batteryTypeRepository;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        // 检查车架号是否已存在
        if (vehicleRepository.existsByCarId(request.getCarId())) {
            throw new BusinessException("ALREADY_EXISTS", "车架号已存在");
        }

        // 获取电池类型
        BatteryType batteryType = getBatteryTypeByCode(request.getBatteryTypeCode());

        // 生成VID (如果请求中未提供)
        String vid = request.getVid();
        if (vid == null || vid.isEmpty()) {
            vid = generateVid();
        }

        // 创建车辆实体
        Vehicle vehicle = Vehicle.builder()
                .vid(vid)
                .carId(request.getCarId())
                .batteryTypeId(batteryType.getId())
                .mileageKm(request.getMileageKm())
                .healthPct(request.getHealthPct())
                .isDelete(false)
                .build();

        // 保存到数据库
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle created: {}", savedVehicle.getVid());

        return buildVehicleResponse(savedVehicle, batteryType);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(String vid) {
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vid));

        BatteryType batteryType = getBatteryTypeById(vehicle.getBatteryTypeId());

        return buildVehicleResponse(vehicle, batteryType);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByCarId(Integer carId) {
        Vehicle vehicle = findVehicleEntity(carId);
        BatteryType batteryType = getBatteryTypeById(vehicle.getBatteryTypeId());

        return buildVehicleResponse(vehicle, batteryType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(vehicle -> {
                    BatteryType batteryType = getBatteryTypeById(vehicle.getBatteryTypeId());
                    return buildVehicleResponse(vehicle, batteryType);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(String vid, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vid));

        // 如果更新了车架号，检查新的车架号是否已被其他车辆使用
        if (!vehicle.getCarId().equals(request.getCarId()) &&
                vehicleRepository.existsByCarId(request.getCarId())) {
            throw new BusinessException("ALREADY_EXISTS", "车架号已存在");
        }

        // 获取电池类型
        BatteryType batteryType = getBatteryTypeByCode(request.getBatteryTypeCode());

        // 更新车辆信息
        vehicle.setCarId(request.getCarId());
        vehicle.setBatteryTypeId(batteryType.getId());
        vehicle.setMileageKm(request.getMileageKm());
        vehicle.setHealthPct(request.getHealthPct());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle updated: {}", updatedVehicle.getVid());

        return buildVehicleResponse(updatedVehicle, batteryType);
    }

    @Override
    @Transactional
    public void deleteVehicle(String vid) {
        Vehicle vehicle = vehicleRepository.findById(vid)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vid));

        vehicle.setIsDelete(true);
        vehicleRepository.save(vehicle);

        log.info("Vehicle deleted: {}", vid);
    }

    @Override
    public Vehicle findVehicleEntity(Integer carId) {
        return vehicleRepository.findByCarId(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with carId", carId.toString()));
    }

    // 辅助方法

    private BatteryType getBatteryTypeByCode(String code) {
        return batteryTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", code));
    }

    private BatteryType getBatteryTypeById(Integer id) {
        return batteryTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BatteryType", id.toString()));
    }

    private String generateVid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private VehicleResponse buildVehicleResponse(Vehicle vehicle, BatteryType batteryType) {
        return VehicleResponse.builder()
                .vid(vehicle.getVid())
                .carId(vehicle.getCarId())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .mileageKm(vehicle.getMileageKm())
                .healthPct(vehicle.getHealthPct())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}