package com.example.allinmarket.common.redis;

import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {
    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final ObjectMapper objectMapper;

    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());

            RealtimeChatMessageDto dto = objectMapper.readValue(body, RealtimeChatMessageDto.class);

            // /sub/chat/room/{roomId}를 구독 중인 사람들에게 메세지 전달
            simpMessageSendingOperations.convertAndSend("/sub/chat/room/" + dto.roomId(), dto);

            log.info("Redis 수진: {}", message);

        } catch (Exception e) {
            log.error("Redis 메세지 처리 실패", e);
        }
    }
}
