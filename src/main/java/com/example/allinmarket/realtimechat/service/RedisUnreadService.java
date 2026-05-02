package com.example.allinmarket.realtimechat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisUnreadService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RealtimeReadStatusService readStatusService;

    // 특정 유저 unread 카운트 증가
    public void incrementUnread(Long roomId, Long userId) {
        String key = generateKey(roomId);

        redisTemplate.opsForHash().increment(key, userId.toString(), 1);

        redisTemplate.expire(key, Duration.ofDays(7));
    }

    // 특정 유저 unread 조회
    public int getUnread(Long roomId, Long userId) {
        String key = generateKey(roomId);
        String field = userId.toString();

        Object value = redisTemplate.opsForHash().get(key, field);

        if (value != null) {
            return Integer.parseInt(value.toString());
        }

        synchronized ((key + field).intern()) {
            Object retry = redisTemplate.opsForHash().get(key, field);

            if (retry != null) {
                return Integer.parseInt(retry.toString());
            }
            int unread = readStatusService.calculateUnreadFromDB(roomId, userId);

            redisTemplate.opsForHash().put(key, field, unread);
            redisTemplate.expire(key, Duration.ofDays(7));

            return unread;
        }
    }

    // 메세지 확인 시 unread 초기화
    public void resetUnread(Long roomId, Long userId) {
        redisTemplate.opsForHash().delete(generateKey(roomId), userId.toString());
    }

    public Map<Object, Object> getAllUnread(Long roomId) {
        return redisTemplate.opsForHash().entries(generateKey(roomId));
    }

    private String generateKey(Long roomId) {
        return "unread:room:" + roomId;
    }
}
