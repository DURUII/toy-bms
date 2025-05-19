package com.mi.bms.signal.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.signal.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "signal-topic";

    public void sendSignal(Signal signal) {
        try {
            String message = objectMapper.writeValueAsString(signal);
            rocketMQTemplate.convertAndSend(TOPIC, message);
            log.info("Signal sent to MQ: {}", signal.getId());
        } catch (Exception e) {
            log.error("Failed to send signal to MQ: {}", signal.getId(), e);
            throw new RuntimeException("Failed to send signal to MQ", e);
        }
    }
}