package com.example.allinmarket.realtimechat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "realtime_chat_read_status")
public class RealtimeReadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;

    private Long userId;

    private Long lastReadMessageId;

    public static RealtimeReadStatus of(Long roomId, Long userId, Long lastReadMessageId) {
        RealtimeReadStatus realtimeReadStatus = new RealtimeReadStatus();
        realtimeReadStatus.roomId = roomId;
        realtimeReadStatus.userId = userId;
        realtimeReadStatus.lastReadMessageId = lastReadMessageId;
        return realtimeReadStatus;
    }

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
