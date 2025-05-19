package com.mi.bms.warning.application.impl;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.service.RuleEngine;
import com.mi.bms.signal.domain.model.Signal;
import com.mi.bms.signal.domain.repository.SignalRepository;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.warning.application.WarningService;
import com.mi.bms.warning.domain.model.Warning;
import com.mi.bms.warning.domain.repository.WarningRepository;
import com.mi.bms.warning.infrastructure.cache.WarningCache;
import com.mi.bms.warning.interfaces.rest.dto.WarningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarningServiceImpl implements WarningService {

    private final SignalRepository signalRepository;
    private final WarningRepository warningRepository;
    private final BatteryTypeRepository batteryTypeRepository;
    private final RuleEngine ruleEngine;
    private final WarningCache warningCache;

    @Override
    @Transactional
    public void generateWarning(Long signalId) {
        // 获取信号
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal not found: " + signalId));

        // 保存信号以更新状态（初始保存）
        signal = signalRepository.save(signal);

        // 获取电池类型
        BatteryType batteryType = batteryTypeRepository.findById(signal.getBatteryTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Battery type not found"));

        // 获取规则列表
        List<WarnRule> rules = ruleEngine.getRulesForBatteryType(batteryType.getId());

        // 评估每个规则
        for (WarnRule rule : rules) {
            Optional<WarnRule.RuleCondition> condition = ruleEngine.evaluateSignal(
                    rule.getRuleNo(),
                    batteryType.getId(),
                    signal.getValues().getVoltageDiff());

            if (condition.isPresent()) {
                // 创建预警
                Warning warning = Warning.create(
                        signal.getCarId(),
                        batteryType.getId(),
                        rule.getRuleNo(),
                        rule.getName(),
                        condition.get().getWarnLevel(),
                        signal.getSignalData());

                // 保存预警
                warning = warningRepository.save(warning);
                log.info("Warning generated: {}", warning.getId());

                // 更新缓存
                warningCache.invalidate(signal.getCarId(), batteryType.getId());
            }
        }

        // 标记信号为已处理
        signal.markAsProcessed();
        signalRepository.save(signal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByCarId(Integer carId, LocalDateTime from, LocalDateTime to) {
        // 尝试从缓存获取
        List<Warning> warnings = warningCache.getByCarId(carId, from, to);
        if (warnings != null) {
            return buildWarningResponses(warnings);
        }

        // 从数据库获取
        warnings = warningRepository.findByCarIdAndTimeRange(carId, from, to);

        // 更新缓存
        warningCache.putByCarId(carId, from, to, warnings);

        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByBatteryType(Integer batteryTypeId, LocalDateTime from, LocalDateTime to) {
        List<Warning> warnings = warningRepository.findByBatteryTypeAndTimeRange(batteryTypeId, from, to);
        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByCarIdAndBatteryType(
            Integer carId, Integer batteryTypeId, LocalDateTime from, LocalDateTime to) {
        List<Warning> warnings = warningRepository.findByCarIdAndBatteryTypeAndTimeRange(
                carId, batteryTypeId, from, to);
        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getAllWarnings(LocalDateTime from, LocalDateTime to) {
        List<Warning> warnings = warningRepository.findByTimeRange(from, to);
        return buildWarningResponses(warnings);
    }

    private List<WarningResponse> buildWarningResponses(List<Warning> warnings) {
        // 获取所有涉及的电池类型
        Map<Integer, BatteryType> batteryTypes = batteryTypeRepository.findAllById(
                warnings.stream()
                        .map(Warning::getBatteryTypeId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(BatteryType::getId, bt -> bt));

        return warnings.stream()
                .map(warning -> buildWarningResponse(warning, batteryTypes.get(warning.getBatteryTypeId())))
                .collect(Collectors.toList());
    }

    private WarningResponse buildWarningResponse(Warning warning, BatteryType batteryType) {
        return WarningResponse.builder()
                .warningId(warning.getId())
                .carId(warning.getCarId())
                .batteryTypeId(batteryType.getId())
                .batteryTypeCode(batteryType.getCode())
                .batteryTypeName(batteryType.getName())
                .ruleNo(warning.getRuleNo())
                .ruleName(warning.getRuleName())
                .warnLevel(warning.getWarnLevel())
                .signalData(warning.getSignalData())
                .createdAt(warning.getCreatedAt())
                .build();
    }
}