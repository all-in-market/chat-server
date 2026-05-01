package com.example.allinmarket.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    // 채팅 메세지를 Redis Pun/Sub으로 발행
    // 모든 서버 인스턴스에게 메세지 전달
    public void publish(Long roomId, Object message) {
        String topic = "chat.room." + roomId;

        redisTemplate.convertAndSend(topic, message);
    }

    public void publishRead(Long roomId, Object message) {
        String topic = "chat.read." + roomId;

        redisTemplate.convertAndSend(topic, message);
    }
}
