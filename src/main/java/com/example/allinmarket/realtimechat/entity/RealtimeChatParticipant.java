package com.example.allinmarket.realtimechat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "realtime_chat_participants")
public class RealtimeChatParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_chat_room_id", nullable = false)
    private RealtimeChatRoom realtimeChatRoom;

    public static RealtimeChatParticipant of (Long userId, RealtimeChatRoom realtimeChatRoom) {
        RealtimeChatParticipant realtimeChatParticipant = new RealtimeChatParticipant();
        realtimeChatParticipant.userId = userId;
        realtimeChatParticipant.realtimeChatRoom = realtimeChatRoom;
        return realtimeChatParticipant;
    }
}
