package com.example.allinmarket.realtimechat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RedisUnreadServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, String, Object> hashOperations;

    @Mock
    private RealtimeReadStatusService readStatusService;

    @InjectMocks
    private RedisUnreadService unreadService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        given(redisTemplate.opsForHash()).willReturn((HashOperations) hashOperations);
    }

    @Test
    void incrementUnread_성공_테스트() {
        // when
        unreadService.incrementUnread(10L, 1L);

        // then
        then(hashOperations).should().increment("unread:room:10", "1", 1);
        then(redisTemplate).should().expire("unread:room:10", Duration.ofDays(7));
    }

    @Test
    void getUnread_캐시값_존재_테스트() {
        given(hashOperations.get("unread:room:10", "1")).willReturn("5");

        int unread = unreadService.getUnread(10L, 1L);

        assertThat(unread).isEqualTo(5);
    }

    @Test
    void getUnread_캐시없음_DB조회_테스트() {
        given(hashOperations.get("unread:room:10", "1")).willReturn(null);
        given(readStatusService.calculateUnreadFromDB(10L, 1L)).willReturn(3);

        int unread = unreadService.getUnread(10L, 1L);

        assertThat(unread).isEqualTo(3);
        then(hashOperations).should().put("unread:room:10", "1", 3);
        then(redisTemplate).should().expire("unread:room:10", Duration.ofDays(7));
    }

    @Test
    void resetUnread_성공_테스트() {
        unreadService.resetUnread(10L, 1L);

        then(hashOperations).should().delete("unread:room:10", "1");
    }

    @Test
    void getAllUnread_성공_테스트() {
        Map<String, Object> mockMap = Map.of("1", 5, "2", 3);
        given(hashOperations.entries("unread:room:10")).willReturn(mockMap);

        Map<Object, Object> result = unreadService.getAllUnread(10L);

        assertThat(result).containsEntry("1", 5).containsEntry("2", 3);
    }
}

