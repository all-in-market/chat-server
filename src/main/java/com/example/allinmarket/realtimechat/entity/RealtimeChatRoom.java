package com.example.allinmarket.realtimechat.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "realtime_chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"buyerId", "sellerId"})
        }
)
public class RealtimeChatRoom extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerId;

    private Long sellerId;

    private String roomName;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    public static RealtimeChatRoom of(Long buyerId, Long sellerId) {
        RealtimeChatRoom realtimeChatRoom = new RealtimeChatRoom();
        realtimeChatRoom.buyerId = buyerId;
        realtimeChatRoom.sellerId = sellerId;
        realtimeChatRoom.roomName = "chat-" + buyerId + "-" + sellerId;
        return realtimeChatRoom;
    }

    public void updateLastMessage(String lastMessage, LocalDateTime lastMessageTime) {
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
}
