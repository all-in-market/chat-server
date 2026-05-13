package com.example.allinmarket.realtimechat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeChatRoomTest {

    @Test
    @DisplayName("of() - buyerId, sellerId 설정 및 roomName 이 'chat-{buyerId}-{sellerId}' 형식으로 생성됨")
    void of_setsFieldsCorrectly() {
        // when
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);

        // then
        assertThat(room.getBuyerId()).isEqualTo(1L);
        assertThat(room.getSellerId()).isEqualTo(2L);
        assertThat(room.getRoomName()).isEqualTo("chat-1-2");
    }

    @Test
    @DisplayName("of() - lastMessage, lastMessageTime 은 초기값 null")
    void of_lastMessageAndTime_areNull() {
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);

        assertThat(room.getLastMessage()).isNull();
        assertThat(room.getLastMessageTime()).isNull();
    }
}