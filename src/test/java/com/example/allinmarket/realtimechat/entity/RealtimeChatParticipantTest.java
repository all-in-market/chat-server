package com.example.allinmarket.realtimechat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeChatParticipantTest {

    @Test
    @DisplayName("of() - userId, realtimeChatRoom 이 올바르게 설정됨")
    void of_setsFieldsCorrectly() {
        // given
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);

        // when
        RealtimeChatParticipant participant = RealtimeChatParticipant.of(1L, room);

        // then
        assertThat(participant.getUserId()).isEqualTo(1L);
        assertThat(participant.getRealtimeChatRoom()).isSameAs(room);
    }

    @Test
    @DisplayName("of() - id 는 persist 전이므로 null")
    void of_idIsNull_beforePersist() {
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);
        RealtimeChatParticipant participant = RealtimeChatParticipant.of(1L, room);

        assertThat(participant.getId()).isNull();
    }
}