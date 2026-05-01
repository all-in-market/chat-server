package com.example.allinmarket.realtimechat.dto;

import com.example.allinmarket.realtimechat.enums.RealtimeMessageType;

public record RealtimeChatMessageDto(
        RealtimeMessageType type,
        Long roomId,
        String senderName,
        String message
) {
}
