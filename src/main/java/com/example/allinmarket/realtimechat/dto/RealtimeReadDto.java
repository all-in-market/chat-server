package com.example.allinmarket.realtimechat.dto;

public record RealtimeReadDto(
        Long roomId,
        Long lastReadMessageId
) {
}
