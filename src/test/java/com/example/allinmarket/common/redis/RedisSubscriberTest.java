package com.example.allinmarket.common.redis;

import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock
    SimpMessageSendingOperations simpMessageSendingOperations;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    RedisSubscriber redisSubscriber;

    // ── 헬퍼: Message 익명 클래스 생성 ─────────────────────────────────────────

    private Message buildMessage(String channel, String body) {
        return new Message() {
            @Override
            public byte[] getChannel() {
                return channel.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    // ── chat.room 채널 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("chat.room 채널 - roomId 일치 → convertAndSend 호출")
    void chatRoom_matchingRoomId_sendsMessage() throws Exception {
        // given
        Long roomId = 10L;
        String channel = "chat.room." + roomId;
        String json = "{\"roomId\":10}";

        RealtimeChatMessageDto dto = mock(RealtimeChatMessageDto.class);
        when(dto.roomId()).thenReturn(roomId);
        when(objectMapper.readValue(json, RealtimeChatMessageDto.class)).thenReturn(dto);

        Message message = buildMessage(channel, json);

        // when
        redisSubscriber.onMessage(message, null);

        // then
        verify(simpMessageSendingOperations)
                .convertAndSend("/sub/chat/room/" + roomId, dto);
    }

    @Test
    @DisplayName("chat.room 채널 - roomId 불일치 → convertAndSend 미호출")
    void chatRoom_mismatchedRoomId_doesNotSend() throws Exception {
        // given
        Long channelRoomId = 10L;
        Long payloadRoomId = 99L;   // 불일치
        String channel = "chat.room." + channelRoomId;
        String json = "{\"roomId\":99}";

        RealtimeChatMessageDto dto = mock(RealtimeChatMessageDto.class);
        when(dto.roomId()).thenReturn(payloadRoomId);
        when(objectMapper.readValue(json, RealtimeChatMessageDto.class)).thenReturn(dto);

        Message message = buildMessage(channel, json);

        // when
        redisSubscriber.onMessage(message, null);

        // then
        verify(simpMessageSendingOperations, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ── chat.read 채널 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("chat.read 채널 - roomId 일치 → convertAndSend 호출")
    void chatRead_matchingRoomId_sendsReadEvent() throws Exception {
        // given
        Long roomId = 20L;
        String channel = "chat.read." + roomId;
        String json = "{\"roomId\":20}";

        RealtimeReadDto dto = mock(RealtimeReadDto.class);
        when(dto.roomId()).thenReturn(roomId);
        when(objectMapper.readValue(json, RealtimeReadDto.class)).thenReturn(dto);

        Message message = buildMessage(channel, json);

        // when
        redisSubscriber.onMessage(message, null);

        // then
        verify(simpMessageSendingOperations)
                .convertAndSend("/sub/chat/read/" + roomId, dto);
    }

    // ── 예외 처리 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("역직렬화 예외 발생 시 예외가 외부로 전파되지 않음")
    void onMessage_deserializationException_doesNotPropagate() throws Exception {
        // given
        String channel = "chat.room.10";
        String json = "{ malformed json }";

        when(objectMapper.readValue(json, RealtimeChatMessageDto.class))
                .thenThrow(new RuntimeException("deserialization failed"));

        Message message = buildMessage(channel, json);

        // when / then – 예외가 전파되지 않고 정상 반환 (내부에서 catch)
        redisSubscriber.onMessage(message, null);

        verify(simpMessageSendingOperations, never()).convertAndSend(anyString(), any(Object.class));
    }
}