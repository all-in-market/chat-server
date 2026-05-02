package com.example.allinmarket.realtimechat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "realtime_chat_read_status",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_realtime_chat_read_status_room_user",
                        columnNames = {"room_id", "user_id"})
        }
)
public class RealtimeReadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_message_id", nullable = false)
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
