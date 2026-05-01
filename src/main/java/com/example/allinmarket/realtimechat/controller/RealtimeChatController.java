package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.example.allinmarket.realtimechat.facade.RealtimeChatFacade;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class RealtimeChatController {
    private final RealtimeChatService chatService;
    private final RealtimeChatFacade chatFacade;

    // /pub/chat/message 경로로 메세지가 오면 실행 됨
    @MessageMapping("/chat/message")
    public void message(RealtimeChatMessageDto dto, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        chatFacade.sendMessage(dto, userId);
    }

    @MessageMapping("/chat/read")
    public void read(RealtimeReadDto dto, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        chatService.read(dto.roomId(), userId, dto.lastReadMessageId());
    }
}
