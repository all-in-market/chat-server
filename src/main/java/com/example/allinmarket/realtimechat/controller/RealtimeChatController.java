package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.enums.RealtimeMessageType;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class RealtimeChatController {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RealtimeChatService realtimeChatService;

    // /pub/chat/message 경로로 메세지가 오면 실행 됨
    @MessageMapping("/chat/message")
    public void message(RealtimeChatMessageDto dto, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        if (RealtimeMessageType.ENTER.equals(dto.type())) {
            // 입장 메세지 (필요시 구현)
        }
        realtimeChatService.validateParticipant(dto.roomId(), userId);

        // ChatService를 호출하여 DB(chat_messages 테이블)에 저장하는 로직 추가
        RealtimeChatMessage saved = realtimeChatService.save(dto, userId);

        RealtimeChatMessageDto response = new RealtimeChatMessageDto(
                dto.type(),
                saved.getRoomId(),
                dto.senderName(),
                saved.getMessage()
        );

        // Redis에 발행
        redisTemplate.convertAndSend("chat.room." + dto.roomId(), response);
    }

    @MessageMapping("/chat/read")
    public void read(RealtimeReadDto dto, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        realtimeChatService.validateParticipant(dto.roomId(), userId);

        realtimeChatService.read(dto.roomId(), userId, dto.lastReadMessageId());
    }
}
