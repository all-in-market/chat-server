package com.example.allinmarket.realtimechat.dto;

import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import java.time.LocalDateTime;

public record RealtimeChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String message,
        LocalDateTime createdAt
) {
    public static RealtimeChatMessageResponse from(RealtimeChatMessage message) {
        return new RealtimeChatMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }
}
