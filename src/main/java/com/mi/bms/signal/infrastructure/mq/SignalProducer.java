package com.mi.bms.signal.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.signal.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "signal-topic";

    @Value("${mq.signal.enabled:false}")
    private boolean mqEnabled;

    public void sendSignal(Signal signal) {
        if (!mqEnabled) {
            log.warn("MQ sending is disabled. Signal {} not sent to queue.", signal.getId());
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(signal);
            try {
                rocketMQTemplate.convertAndSend(TOPIC, message);
                log.info("Signal sent to MQ: {}", signal.getId());
            } catch (Exception e) {
                log.warn("Failed to send signal to MQ (continuing anyway): {}, reason: {}",
                        signal.getId(), e.getMessage());
                log.debug("Detailed MQ error", e);
            }
        } catch (Exception e) {
            log.error("Failed to serialize signal: {}", signal.getId(), e);
            // Don't throw exception to allow the process to continue
        }
    }
}