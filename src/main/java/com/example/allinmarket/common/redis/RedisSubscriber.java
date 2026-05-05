package com.example.allinmarket.common.redis;

import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {
    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final ObjectMapper objectMapper;

    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());

            String json = new String(message.getBody(), StandardCharsets.UTF_8);

            if (channel.startsWith("chat.room.")) {
                RealtimeChatMessageDto dto = objectMapper.readValue(json, RealtimeChatMessageDto.class);

                Long roomIdFromChannel = extractRoomId(channel, "chat.room.");

                if (!roomIdFromChannel.equals(dto.roomId())) {
                    log.warn("roomId가 올바르지 않습니다 (message). channel = {}, payload = {}", roomIdFromChannel, dto.roomId());
                    return;
                }

                // /sub/chat/room/{roomId}를 구독 중인 사람들에게 메세지 전달
                simpMessageSendingOperations.convertAndSend("/sub/chat/room/" + dto.roomId(), dto);

                log.info("Redis 수신: channel = {}, roomId = {}", channel, roomIdFromChannel);
            }

            if (channel.startsWith("chat.read.")) {
                RealtimeReadDto dto = objectMapper.readValue(json, RealtimeReadDto.class);

                Long roomIdFromChannel = extractRoomId(channel, "chat.read.");

                if (!roomIdFromChannel.equals(dto.roomId())) {
                    log.warn("roomId가 올바르지 않습니다 (read). channel = {}, payload = {}", roomIdFromChannel, dto.roomId());
                    return;
                }

                simpMessageSendingOperations.convertAndSend("/sub/chat/read/" + dto.roomId(), dto);

                log.info("Redis 수신: channel = {}, roomId = {}", channel, roomIdFromChannel);
            }

        } catch (Exception e) {
            log.error("Redis 메세지 처리 실패", e);
        }
    }

    private Long extractRoomId(String channel, String prefix) {
        try {
            return Long.parseLong(channel.substring(prefix.length()));

        } catch (Exception e) {
            log.error("유표하지 않은 채널 입니다: {}", channel, e);
            throw e;
        }
    }
}
