package com.mi.bms.signal.interfaces.rest.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class SignalRequest {

    @NotNull(message = "车架编号不能为空")
    private Integer carId;

    private Integer warnId; // 规则编号，可选

    @NotNull(message = "信号数据不能为空")
    @Pattern(regexp = "^\\{\"Mx\":[0-9.]+,\"Mi\":[0-9.]+(,\"Ix\":[0-9.]+,\"Ii\":[0-9.]+)?\\}$", message = "信号数据格式不正确")
    private String signal; // JSON 格式的信号数据
}