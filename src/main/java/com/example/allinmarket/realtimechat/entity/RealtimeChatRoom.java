package com.example.allinmarket.realtimechat.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
                @UniqueConstraint(
                        name = "uk_realtime_chat_room_buyer_seller",
                        columnNames = {"buyer_id", "seller_id"})
        }
)
public class RealtimeChatRoom extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @NotBlank
    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    public static RealtimeChatRoom of(Long buyerId, Long sellerId) {
        RealtimeChatRoom realtimeChatRoom = new RealtimeChatRoom();
        realtimeChatRoom.buyerId = buyerId;
        realtimeChatRoom.sellerId = sellerId;
        realtimeChatRoom.roomName = "chat-" + buyerId + "-" + sellerId;
        return realtimeChatRoom;
    }
}
