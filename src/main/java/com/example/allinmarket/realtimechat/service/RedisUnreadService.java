package com.example.allinmarket.realtimechat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisUnreadService {
    private final RedisTemplate<String, Object> redisTemplate;

    // 특정 유저 unread 카운트 증가
    public void incrementUnread(Long roomId, Long userId) {
        redisTemplate.opsForHash().increment(generateKey(roomId), userId.toString(), 1);
    }

    // 특정 유저 unread 조회
    public int getUnread(Long roomId, Long userId) {
        Object value = redisTemplate.opsForHash().get(generateKey(roomId), userId.toString());

        return value == null ? 0 : Integer.parseInt(value.toString());
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
