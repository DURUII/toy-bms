package com.mi.bms.warning.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.signal.domain.model.Signal;
import com.mi.bms.warning.application.WarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "signal-topic", consumerGroup = "warning-consumer-group")
public class SignalConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final WarningService warningService;

    @Override
    public void onMessage(String message) {
        try {
            Signal signal = objectMapper.readValue(message, Signal.class);
            log.info("Received signal message: {}", signal.getId());

            // 生成预警
            warningService.generateWarning(signal.getId());
        } catch (Exception e) {
            log.error("Failed to process signal message: {}", message, e);
            // TODO: 考虑重试或死信队列
        }
    }
}