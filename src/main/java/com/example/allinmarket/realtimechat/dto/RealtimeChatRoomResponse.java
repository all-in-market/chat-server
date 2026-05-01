package com.example.allinmarket.realtimechat.dto;

import java.time.LocalDateTime;

public record RealtimeChatRoomResponse(
        Long roomId,
        String roomName,
        String lastMessage,
        LocalDateTime lastMessageTime,
        int unreadCount,
        String opponentName
) {
}
