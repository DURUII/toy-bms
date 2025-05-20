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
import com.mi.bms.warning.application.WarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final PlatformTransactionManager transactionManager;

    // 可选: 直接注入WarningService以支持同步处理
    private final WarningService warningService;

    @Value("${mq.signal.enabled:false}")
    private boolean mqEnabled;

    @Value("${direct.signal.processing:true}")
    private boolean directProcessingEnabled;

    @Override
    public List<SignalResponse> reportSignals(@Valid List<SignalRequest> requests) {
        log.info("==== SIGNAL REPORTING START ==== Received {} signal requests", requests.size());
        List<SignalResponse> responses = new ArrayList<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (SignalRequest request : requests) {
            try {
                log.info("Processing signal for carId: {}, signal data: {}", request.getCarId(), request.getSignal());

                // Process each signal in its own transaction
                SignalResponse response = transactionTemplate.execute(status -> {
                    try {
                        // 获取车辆信息
                        var vehicle = vehicleDomainService.getVehicleByCarId(request.getCarId());
                        log.info("Found vehicle with VID: {}, batteryTypeId: {}", vehicle.getVid(),
                                vehicle.getBatteryTypeId());

                        var batteryType = batteryTypeRepository.findById(vehicle.getBatteryTypeId())
                                .orElseThrow(() -> new IllegalArgumentException("Battery type not found"));
                        log.info("Found battery type: {}, name: {}", batteryType.getCode(), batteryType.getName());

                        // 解析信号数据
                        Map<String, Object> rawValues;
                        try {
                            rawValues = objectMapper.readValue(
                                    request.getSignal(),
                                    Map.class);
                            log.info("Parsed signal raw values: {}", rawValues);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to parse signal data: {}", request.getSignal(), e);
                            throw new IllegalArgumentException("Invalid signal data format");
                        }

                        // Convert Double values to BigDecimal
                        Map<String, BigDecimal> signalValues = new HashMap<>();
                        for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
                            if (entry.getValue() != null) {
                                if (entry.getValue() instanceof Number) {
                                    signalValues.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
                                } else {
                                    log.warn("Ignoring non-numeric value for key {}: {}", entry.getKey(),
                                            entry.getValue());
                                }
                            }
                        }

                        log.info("Converted to BigDecimal values: {}", signalValues);

                        // 创建信号
                        Signal signal = Signal.create(
                                request.getCarId(),
                                batteryType.getId(),
                                signalValues);
                        log.info("Created signal object: {}", signal);

                        // 保存信号
                        signal = signalRepository.save(signal);
                        log.info("Signal saved to database with ID: {}", signal.getId());

                        // Build response within transaction
                        return buildSignalResponse(signal, batteryType);
                    } catch (Exception e) {
                        log.error("Transaction error processing signal for carId: {}", request.getCarId(), e);
                        // Mark for rollback explicitly
                        status.setRollbackOnly();
                        throw e;
                    }
                });

                // If we get here, the transaction completed successfully
                if (response != null) {
                    responses.add(response);
                    log.info("Added signal response: {}", response);

                    // These operations are now outside the transaction
                    try {
                        // Get the signal ID from the response
                        Long signalId = response.getSignalId();

                        // 发送到 MQ (outside transaction)
                        Signal signal = signalRepository.findById(signalId)
                                .orElseThrow(() -> new IllegalStateException("Signal not found after saving"));
                        signalProducer.sendSignal(signal);

                        // 直接处理（用于测试，当MQ不可用时）
                        if (directProcessingEnabled && !mqEnabled) {
                            try {
                                log.info("MQ is disabled, processing signal directly: {}", signalId);
                                warningService.generateWarning(signalId);
                                log.info("Direct signal processing completed for ID: {}", signalId);
                            } catch (Exception e) {
                                log.error("Error during direct signal processing: {}", e.getMessage(), e);
                                // Non-critical error, don't rethrow
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error in post-transaction processing for carId: {}", request.getCarId(), e);
                        // Don't rethrow as it's outside the main transaction
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error processing signal for carId: {}", request.getCarId(), e);
                // Continue processing other requests instead of failing the entire batch
            }
        }

        log.info("==== SIGNAL REPORTING COMPLETE ==== Processed {} signals", responses.size());
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalResponse> getSignalsByCarId(Integer carId, LocalDateTime from, LocalDateTime to) {
        log.info("Querying signals for carId: {}, from: {}, to: {}", carId, from, to);
        List<Signal> signals = signalRepository.findByCarIdAndTimeRange(carId, from, to);
        log.info("Found {} signals for carId: {}", signals.size(), carId);
        return buildSignalResponses(signals);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalResponse> getAllSignals(LocalDateTime from, LocalDateTime to) {
        log.info("Querying all signals from: {}, to: {}", from, to);
        List<Signal> signals = signalRepository.findByTimeRange(from, to);
        log.info("Found {} signals in the time range", signals.size());
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