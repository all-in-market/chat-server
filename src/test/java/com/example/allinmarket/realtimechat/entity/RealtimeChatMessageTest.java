package com.example.allinmarket.realtimechat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeChatMessageTest {

    @Test
    @DisplayName("of() - roomId, senderId, message 가 올바르게 설정됨")
    void of_setsFieldsCorrectly() {
        // when
        RealtimeChatMessage chatMessage = RealtimeChatMessage.of(10L, 1L, "안녕하세요");

        // then
        assertThat(chatMessage.getRoomId()).isEqualTo(10L);
        assertThat(chatMessage.getSenderId()).isEqualTo(1L);
        assertThat(chatMessage.getMessage()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("of() - id 는 persist 전이므로 null")
    void of_idIsNull_beforePersist() {
        RealtimeChatMessage chatMessage = RealtimeChatMessage.of(10L, 1L, "hello");

        assertThat(chatMessage.getId()).isNull();
    }
}