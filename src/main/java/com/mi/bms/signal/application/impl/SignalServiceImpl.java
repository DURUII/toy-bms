package com.mi.bms.signal.application.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.signal.application.SignalService;
import com.mi.bms.signal.domain.model.Signal;
import com.mi.bms.signal.domain.repository.SignalRepository;
import com.mi.bms.signal.infrastructure.mq.SignalProducer;
import com.mi.bms.signal.interfaces.rest.dto.SignalRequest;
import com.mi.bms.signal.interfaces.rest.dto.SignalResponse;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.domain.service.VehicleDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalServiceImpl implements SignalService {

    private final SignalRepository signalRepository;
    private final VehicleDomainService vehicleDomainService;
    private final BatteryTypeRepository batteryTypeRepository;
    private final SignalProducer signalProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<SignalResponse> reportSignals(@Valid List<SignalRequest> requests) {
        List<SignalResponse> responses = new ArrayList<>();

        for (SignalRequest request : requests) {
            try {
                // 获取车辆信息
                var vehicle = vehicleDomainService.getVehicleByCarId(request.getCarId());
                var batteryType = batteryTypeRepository.findById(vehicle.getBatteryTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Battery type not found"));

                // 解析信号数据
                Map<String, BigDecimal> signalValues = objectMapper.readValue(
                        request.getSignal(),
                        Map.class);

                // 创建信号
                Signal signal = Signal.create(
                        request.getCarId(),
                        batteryType.getId(),
                        signalValues);

                // 保存信号
                signal = signalRepository.save(signal);
                log.info("Signal saved: {}", signal.getId());

                // 发送到 MQ
                signalProducer.sendSignal(signal);

                // 构建响应
                responses.add(buildSignalResponse(signal, batteryType));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse signal data: {}", request.getSignal(), e);
                throw new IllegalArgumentException("Invalid signal data format");
            }
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalResponse> getSignalsByCarId(Integer carId, LocalDateTime from, LocalDateTime to) {
        List<Signal> signals = signalRepository.findByCarIdAndTimeRange(carId, from, to);
        return buildSignalResponses(signals);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalResponse> getAllSignals(LocalDateTime from, LocalDateTime to) {
        List<Signal> signals = signalRepository.findByTimeRange(from, to);
        return buildSignalResponses(signals);
    }

    private List<SignalResponse> buildSignalResponses(List<Signal> signals) {
        // 获取所有涉及的电池类型
        Map<Integer, BatteryType> batteryTypes = batteryTypeRepository.findAllById(
                signals.stream()
                        .map(Signal::getBatteryTypeId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(BatteryType::getId, bt -> bt));

        return signals.stream()
                .map(signal -> buildSignalResponse(signal, batteryTypes.get(signal.getBatteryTypeId())))
                .collect(Collectors.toList());
    }

    private SignalResponse buildSignalResponse(Signal signal, BatteryType batteryType) {
        return SignalResponse.builder()
                .signalId(signal.getId())
                .carId(signal.getCarId())
                .batteryTypeId(batteryType.getId())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .signalData(signal.getSignalData())
                .processed(signal.isProcessed())
                .createdAt(signal.getCreatedAt())
                .build();
    }
}