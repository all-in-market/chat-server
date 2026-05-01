package com.example.allinmarket.realtimechat.dto;

import com.example.allinmarket.realtimechat.enums.RealtimeMessageType;

import java.util.Map;

public record RealtimeChatMessageDto(
        RealtimeMessageType type,
        Long roomId,
        String senderName,
        String message,
        Map<Long, Integer> unreadCounts
) {
}
