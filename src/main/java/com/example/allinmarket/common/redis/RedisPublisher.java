package com.example.allinmarket.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    // 채팅 메세지를 Redis Pun/Sub으로 발행
    // 모든 서버 인스턴스에게 메세지 전달
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public void publish(Long roomId, Object message) {
        String topic = "chat.room." + roomId;

        redisTemplate.convertAndSend(topic, message);
    }

    public void publishRead(Long roomId, Object message) {
        String topic = "chat.read." + roomId;

        redisTemplate.convertAndSend(topic, message);
    }

    @Recover // 재시도 실패 시 최종 실행 되는 로직
    public void recover(Exception e, Long roomId, Object message) {
        log.error("Redis 발행 최종 실패: roomId = {}, Error = {}", roomId, e.getMessage(), e);
    }
}
