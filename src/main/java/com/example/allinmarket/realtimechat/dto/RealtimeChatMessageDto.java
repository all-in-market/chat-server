package com.example.allinmarket.realtimechat.dto;

import com.example.allinmarket.realtimechat.enums.RealtimeChatMessageType;

import java.util.Map;

public record RealtimeChatMessageDto(
        RealtimeChatMessageType type,
        Long roomId,
        String senderName,
        String message,
        Map<Long, Integer> unreadCounts,
        String tempId
) {
}
