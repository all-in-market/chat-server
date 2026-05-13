package com.example.allinmarket.realtimechat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeReadStatusTest {

    // ── of() 팩토리 메서드 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("of() - 전달한 roomId, userId, lastReadMessageId 가 그대로 설정된다")
    void of_setsFieldsCorrectly() {
        // when
        RealtimeReadStatus status = RealtimeReadStatus.of(1L, 2L, 100L);

        // then
        assertThat(status.getRoomId()).isEqualTo(1L);
        assertThat(status.getUserId()).isEqualTo(2L);
        assertThat(status.getLastReadMessageId()).isEqualTo(100L);
    }

    // ── updateLastRead ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateLastRead(큰 값) - lastReadMessageId 가 새 값으로 업데이트된다")
    void updateLastRead_withLargerValue_updates() {
        // given
        RealtimeReadStatus status = RealtimeReadStatus.of(1L, 1L, 100L);

        // when
        status.updateLastRead(200L);

        // then
        assertThat(status.getLastReadMessageId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("updateLastRead(작은 값) - lastReadMessageId 가 기존 값으로 유지된다")
    void updateLastRead_withSmallerValue_doesNotUpdate() {
        // given
        RealtimeReadStatus status = RealtimeReadStatus.of(1L, 1L, 100L);

        // when
        status.updateLastRead(50L);

        // then
        assertThat(status.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("updateLastRead(같은 값) - lastReadMessageId 가 기존 값으로 유지된다")
    void updateLastRead_withSameValue_doesNotUpdate() {
        // given
        RealtimeReadStatus status = RealtimeReadStatus.of(1L, 1L, 100L);

        // when
        status.updateLastRead(100L);

        // then
        assertThat(status.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("updateLastRead(null) - lastReadMessageId 가 기존 값으로 유지된다")
    void updateLastRead_withNull_doesNotUpdate() {
        // given
        RealtimeReadStatus status = RealtimeReadStatus.of(1L, 1L, 100L);

        // when
        status.updateLastRead(null);

        // then
        assertThat(status.getLastReadMessageId()).isEqualTo(100L);
    }
}