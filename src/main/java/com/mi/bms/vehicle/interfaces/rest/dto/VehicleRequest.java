package com.mi.bms.vehicle.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {

    private String vid;

    @NotNull(message = "车架编号不能为空")
    private Integer carId;

    @NotBlank(message = "电池类型编码不能为空")
    private String batteryTypeCode;

    @NotNull(message = "里程不能为空")
    @Min(value = 0, message = "里程不能为负数")
    private Long mileageKm;

    @NotNull(message = "电池健康状态不能为空")
    @Min(value = 0, message = "电池健康状态必须大于等于0")
    private Integer healthPct;
}