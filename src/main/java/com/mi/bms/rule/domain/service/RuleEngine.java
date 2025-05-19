package com.mi.bms.rule.domain.service;

import com.mi.bms.rule.domain.model.WarnRule;
import com.mi.bms.rule.domain.model.WarnRuleItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 规则引擎，用于评估信号是否触发预警
 */
@Slf4j
@Service
public class RuleEngine {

    /**
     * 评估信号是否触发规则预警
     * 
     * @param rule       预警规则
     * @param signalData 信号数据Map
     * @return 如触发预警则返回预警级别，否则返回null
     */
    public Integer evaluateRule(WarnRule rule, Map<String, BigDecimal> signalData) {
        // 1. 根据表达式计算差值
        BigDecimal difference = calculateDifference(rule.getExpr(), signalData);
        if (difference == null) {
            log.warn("Cannot calculate difference for rule: {}, expr: {}", rule.getId(), rule.getExpr());
            return null;
        }

        // 2. 找到匹配的区间
        Optional<WarnRuleItem> matchedItem = rule.getItems().stream()
                .filter(item -> item.isInRange(difference))
                .findFirst();

        // 3. 如果没有匹配区间或预警级别为null，则不预警
        return matchedItem.map(WarnRuleItem::getWarnLevel).orElse(null);
    }

    /**
     * 根据表达式和信号数据计算差值
     */
    private BigDecimal calculateDifference(String expr, Map<String, BigDecimal> signalData) {
        switch (expr) {
            case "MX_MI": // 电压差
                BigDecimal mx = signalData.get("Mx");
                BigDecimal mi = signalData.get("Mi");
                if (mx == null || mi == null) {
                    return null;
                }
                return mx.subtract(mi).setScale(2, RoundingMode.HALF_UP);

            case "IX_II": // 电流差
                BigDecimal ix = signalData.get("Ix");
                BigDecimal ii = signalData.get("Ii");
                if (ix == null || ii == null) {
                    return null;
                }
                return ix.subtract(ii).setScale(2, RoundingMode.HALF_UP);

            default:
                log.error("Unsupported expression: {}", expr);
                return null;
        }
    }

    /**
     * 解析信号字符串为Map
     */
    public Map<String, BigDecimal> parseSignalJson(String signalJson) {
        Map<String, BigDecimal> result = new HashMap<>();

        // 简单解析，支持形如 {"Mx":12.0,"Mi":0.6} 的格式
        String trimmed = signalJson.trim().replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = trimmed.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                try {
                    BigDecimal value = new BigDecimal(keyValue[1].trim());
                    result.put(key, value);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse value for key {}: {}", keyValue[0], keyValue[1]);
                }
            }
        }

        return result;
    }

    /**
     * 批量评估信号是否触发一组规则预警
     * 
     * @return 返回触发的规则ID与预警级别的映射
     */
    public Map<Long, Integer> evaluateRules(List<WarnRule> rules, Map<String, BigDecimal> signalData) {
        Map<Long, Integer> warningResults = new HashMap<>();

        for (WarnRule rule : rules) {
            Integer warnLevel = evaluateRule(rule, signalData);
            if (warnLevel != null) {
                warningResults.put(rule.getId(), warnLevel);
            }
        }

        return warningResults;
    }
}