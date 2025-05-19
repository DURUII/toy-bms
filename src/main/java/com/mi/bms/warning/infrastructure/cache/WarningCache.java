package com.mi.bms.warning.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mi.bms.warning.domain.model.Warning;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarningCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "warning:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final long CACHE_TTL = 1; // 1 hour

    public List<Warning> getByCarId(Integer carId, LocalDateTime from, LocalDateTime to) {
        String key = buildKey(carId, from, to);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, new TypeReference<List<Warning>>() {
            });
        } catch (Exception e) {
            log.error("Failed to deserialize warnings from cache: {}", key, e);
            return null;
        }
    }

    public void putByCarId(Integer carId, LocalDateTime from, LocalDateTime to, List<Warning> warnings) {
        String key = buildKey(carId, from, to);
        try {
            String value = objectMapper.writeValueAsString(warnings);
            redisTemplate.opsForValue().set(key, value, CACHE_TTL, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to serialize warnings to cache: {}", key, e);
        }
    }

    public void invalidate(Integer carId, Integer batteryTypeId) {
        String pattern = KEY_PREFIX + carId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.info("Invalidated warning cache for carId: {}", carId);
    }

    private String buildKey(Integer carId, LocalDateTime from, LocalDateTime to) {
        return KEY_PREFIX + carId + ":" + from.format(FORMATTER) + ":" + to.format(FORMATTER);
    }
}