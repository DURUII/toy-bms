package com.mi.bms.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatteryType {
    TERNARY("三元电池"),
    LFP("铁锂电池");

    private final String name;
}