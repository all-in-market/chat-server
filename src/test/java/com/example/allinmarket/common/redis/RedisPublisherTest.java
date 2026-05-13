package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    RedisPublisher redisPublisher;

    // ── publish ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publish 성공 시 redisTemplate.convertAndSend 호출")
    void publish_success() throws Exception {
        // given
        Long roomId = 1L;
        Object message = new Object();
        String json = "{\"key\":\"value\"}";

        when(objectMapper.writeValueAsString(message)).thenReturn(json);

        // when
        redisPublisher.publish(roomId, message);

        // then
        verify(redisTemplate).convertAndSend("chat.room." + roomId, json);
    }

    @Test
    @DisplayName("publish 직렬화 실패 시 BaseException(REDIS_PUBLISH_FAILED) 발생")
    void publish_serializationFail_throwsBaseException() throws Exception {
        // given
        Long roomId = 1L;
        Object message = new Object();

        when(objectMapper.writeValueAsString(message))
                .thenThrow(new JsonProcessingException("serialize error") {});

        // when / then
        assertThatThrownBy(() -> redisPublisher.publish(roomId, message))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.REDIS_PUBLISH_FAILED));

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    // ── publishRead ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishRead 성공 시 redisTemplate.convertAndSend 호출")
    void publishRead_success() throws Exception {
        // given
        Long roomId = 2L;
        Object message = new Object();
        String json = "{\"roomId\":2}";

        when(objectMapper.writeValueAsString(message)).thenReturn(json);

        // when
        redisPublisher.publishRead(roomId, message);

        // then
        verify(redisTemplate).convertAndSend("chat.read." + roomId, json);
    }

    @Test
    @DisplayName("publishRead 직렬화 실패 시 BaseException(REDIS_PUBLISH_READ_FAILED) 발생")
    void publishRead_serializationFail_throwsBaseException() throws Exception {
        // given
        Long roomId = 2L;
        Object message = new Object();

        when(objectMapper.writeValueAsString(message))
                .thenThrow(new JsonProcessingException("serialize error") {});

        // when / then
        assertThatThrownBy(() -> redisPublisher.publishRead(roomId, message))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.REDIS_PUBLISH_READ_FAILED));

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    // ── recover ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("recover 호출 시 전달받은 예외를 그대로 재전파")
    void recover_rethrowsException() {
        // given
        Exception cause = new RuntimeException("Redis 연결 실패");

        // when / then
        assertThatThrownBy(() -> redisPublisher.recover(cause, 1L, new Object()))
                .isSameAs(cause);
    }
}