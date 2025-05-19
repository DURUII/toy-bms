package com.mi.bms.vehicle.application;

import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleResponse;

import java.util.List;

public interface VehicleService {

    /**
     * 创建车辆
     */
    VehicleResponse createVehicle(VehicleRequest request);

    /**
     * 根据ID查询车辆
     */
    VehicleResponse getVehicleById(String vid);

    /**
     * 根据车架号查询车辆
     */
    VehicleResponse getVehicleByCarId(Integer carId);

    /**
     * 查询所有车辆
     */
    List<VehicleResponse> getAllVehicles();

    /**
     * 更新车辆信息
     */
    VehicleResponse updateVehicle(String vid, VehicleRequest request);

    /**
     * 删除车辆
     */
    void deleteVehicle(String vid);

    /**
     * 内部方法，查找车辆实体
     */
    Vehicle findVehicleEntity(Integer carId);
}