package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RealtimeReadStatusServiceTest {

    @Mock
    private RealtimeReadStatusRepository readStatusRepository;

    @Mock
    private RealtimeChatMessageRepository chatMessageRepository;

    @InjectMocks
    private RealtimeReadStatusService readStatusService;

    @Test
    void 읽음_상태_존재_할때_unread_계산_성공() {
        // given
        RealtimeReadStatus status = RealtimeReadStatus.of(10L, 1L, 5L);
        given(readStatusRepository.findByRoomIdAndUserId(10L, 1L))
                .willReturn(Optional.of(status));
        given(chatMessageRepository.countUnreadMessages(10L, 5L))
                .willReturn(3);

        // when
        int unread = readStatusService.calculateUnreadFromDB(10L, 1L);

        // then
        assertThat(unread).isEqualTo(3);
        then(chatMessageRepository).should().countUnreadMessages(10L, 5L);
    }

    @Test
    void 읽음_상태_없을때_unread_계산_성공() {
        // given
        given(readStatusRepository.findByRoomIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
        given(chatMessageRepository.countUnreadMessages(10L, 0L))
                .willReturn(7);

        // when
        int unread = readStatusService.calculateUnreadFromDB(10L, 1L);

        // then
        assertThat(unread).isEqualTo(7);
        then(chatMessageRepository).should().countUnreadMessages(10L, 0L);
    }
}

