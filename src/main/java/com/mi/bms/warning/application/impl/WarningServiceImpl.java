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
        log.info("==== WARNING GENERATION START ==== Processing signal ID: {}", signalId);

        // 获取信号
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal not found: " + signalId));
        log.info("Retrieved signal with ID: {}, carId: {}, batteryTypeId: {}, processed: {}",
                signal.getId(), signal.getCarId(), signal.getBatteryTypeId(), signal.isProcessed());

        if (signal.isProcessed()) {
            log.info("Signal already processed, skipping: {}", signalId);
            return;
        }

        // 获取电池类型
        BatteryType batteryType = batteryTypeRepository.findById(signal.getBatteryTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Battery type not found"));
        log.info("Retrieved battery type: {}, name: {}", batteryType.getCode(), batteryType.getName());

        // 获取规则列表
        List<WarnRule> rules = ruleEngine.getRulesForBatteryType(batteryType.getId());
        log.info("Found {} rules for battery type ID: {}", rules.size(), batteryType.getId());

        int warningsGenerated = 0;

        // 评估每个规则
        for (WarnRule rule : rules) {
            log.info("Evaluating rule: {}, ruleNo: {}, expr: {}",
                    rule.getId(), rule.getRuleNo(), rule.getExpr());

            Optional<WarnRule.RuleCondition> condition = ruleEngine.evaluateSignal(
                    rule.getRuleNo(),
                    batteryType.getId(),
                    signal.getValues().getVoltageDiff());

            if (condition.isPresent()) {
                log.info("Rule condition matched: warnLevel={}", condition.get().getWarnLevel());

                // 创建预警
                Warning warning = Warning.create(
                        signal.getCarId(),
                        batteryType.getId(),
                        rule.getRuleNo(),
                        rule.getName(),
                        condition.get().getWarnLevel(),
                        signal.getSignalData());
                log.info("Created warning entity: {}", warning);

                // 保存预警
                warning = warningRepository.save(warning);
                log.info("Warning saved to database with ID: {}", warning.getId());
                warningsGenerated++;

                // 更新缓存
                warningCache.invalidate(signal.getCarId(), batteryType.getId());
                log.info("Cache invalidated for carId: {}, batteryTypeId: {}",
                        signal.getCarId(), batteryType.getId());
            } else {
                log.info("No rule condition matched for rule: {}", rule.getId());
            }
        }

        // 标记信号为已处理
        signal.markAsProcessed();
        signalRepository.save(signal);
        log.info("Signal marked as processed: {}", signal.getId());

        log.info("==== WARNING GENERATION COMPLETE ==== Generated {} warnings for signal ID: {}",
                warningsGenerated, signalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByCarId(Integer carId, LocalDateTime from, LocalDateTime to) {
        log.info("Querying warnings for carId: {}, from: {}, to: {}", carId, from, to);

        // 尝试从缓存获取
        List<Warning> warnings = warningCache.getByCarId(carId, from, to);
        if (warnings != null) {
            log.info("Cache hit! Found {} warnings in cache for carId: {}", warnings.size(), carId);
            return buildWarningResponses(warnings);
        }

        // 从数据库获取
        log.info("Cache miss. Querying database for carId: {}", carId);
        warnings = warningRepository.findByCarIdAndTimeRange(carId, from, to);
        log.info("Found {} warnings in database for carId: {}", warnings.size(), carId);

        // 更新缓存
        warningCache.putByCarId(carId, from, to, warnings);
        log.info("Updated cache for carId: {}", carId);

        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByBatteryType(Integer batteryTypeId, LocalDateTime from, LocalDateTime to) {
        log.info("Querying warnings for batteryTypeId: {}, from: {}, to: {}", batteryTypeId, from, to);
        List<Warning> warnings = warningRepository.findByBatteryTypeAndTimeRange(batteryTypeId, from, to);
        log.info("Found {} warnings for batteryTypeId: {}", warnings.size(), batteryTypeId);
        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByCarIdAndBatteryType(
            Integer carId, Integer batteryTypeId, LocalDateTime from, LocalDateTime to) {
        log.info("Querying warnings for carId: {}, batteryTypeId: {}, from: {}, to: {}",
                carId, batteryTypeId, from, to);
        List<Warning> warnings = warningRepository.findByCarIdAndBatteryTypeAndTimeRange(
                carId, batteryTypeId, from, to);
        log.info("Found {} warnings for carId: {} and batteryTypeId: {}",
                warnings.size(), carId, batteryTypeId);
        return buildWarningResponses(warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarningResponse> getAllWarnings(LocalDateTime from, LocalDateTime to) {
        log.info("Querying all warnings from: {}, to: {}", from, to);
        List<Warning> warnings = warningRepository.findByTimeRange(from, to);
        log.info("Found {} warnings in the time range", warnings.size());
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