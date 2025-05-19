package com.mi.bms.warning.application;

import com.mi.bms.warning.interfaces.rest.dto.WarningResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface WarningService {

    /**
     * 生成预警
     */
    void generateWarning(Long signalId);

    /**
     * 查询指定车辆的预警
     */
    List<WarningResponse> getWarningsByCarId(Integer carId, LocalDateTime from, LocalDateTime to);

    /**
     * 查询指定电池类型的预警
     */
    List<WarningResponse> getWarningsByBatteryType(Integer batteryTypeId, LocalDateTime from, LocalDateTime to);

    /**
     * 查询指定车辆和电池类型的预警
     */
    List<WarningResponse> getWarningsByCarIdAndBatteryType(
            Integer carId, Integer batteryTypeId, LocalDateTime from, LocalDateTime to);

    /**
     * 查询所有预警
     */
    List<WarningResponse> getAllWarnings(LocalDateTime from, LocalDateTime to);
}