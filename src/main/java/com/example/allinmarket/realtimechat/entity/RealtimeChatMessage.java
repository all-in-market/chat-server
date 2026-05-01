package com.example.allinmarket.realtimechat.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "realtime_chat_messages")
public class RealtimeChatMessage extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;

    private Long senderId;

    private String message;

    public static RealtimeChatMessage of (Long roomId, Long senderId, String message) {
        RealtimeChatMessage realtimeChatMessage = new RealtimeChatMessage();

        realtimeChatMessage.roomId = roomId;
        realtimeChatMessage.senderId = senderId;
        realtimeChatMessage.message = message;
        return realtimeChatMessage;
    }
}
