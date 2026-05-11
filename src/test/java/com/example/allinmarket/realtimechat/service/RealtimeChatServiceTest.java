package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RealtimeChatServiceTest {

    @Mock
    private RealtimeChatMessageRepository chatMessageRepository;

    @Mock
    private RealtimeReadStatusRepository readStatusRepository;

    @Mock
    private RealtimeChatParticipantRepository chatParticipantRepository;

    @Mock
    private RealtimeChatRoomRepository chatRoomRepository;

    @Mock
    private RedisUnreadService unreadService;

    @Mock
    private RedisPublisher redisPublisher;

    @InjectMocks
    private RealtimeChatService chatService;

    @Test
    void 메시지_저장_성공_테스트() {
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                null, 10L, "이름", "안녕하세요", null, "temp-1"
        );
        RealtimeChatMessage saved = RealtimeChatMessage.of(10L, 1L, "안녕하세요");

        given(chatMessageRepository.save(any())).willReturn(saved);

        RealtimeChatMessage result = chatService.save(dto, 1L);

        then(chatMessageRepository).should().save(any(RealtimeChatMessage.class));
    }

    @Test
    void 읽음_처리_성공_테스트() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(true);
            given(chatMessageRepository.existsByIdAndRoomId(10L, 5L)).willReturn(true);
            given(readStatusRepository.findByRoomIdAndUserId(10L, 1L)).willReturn(Optional.of(RealtimeReadStatus.of(10L, 1L, 0L)));

            chatService.read(10L, 1L, 5L);

            // afterCommit 콜백 강제 실행
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            then(readStatusRepository).should().save(any(RealtimeReadStatus.class));
            then(unreadService).should().resetUnread(10L, 1L);
            then(redisPublisher).should().publishRead(eq(10L), any(RealtimeReadDto.class));

        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void 읽음_처리_실패_테스트_메시지없음() {
        given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(true);
        given(chatMessageRepository.existsByIdAndRoomId(10L, 5L)).willReturn(false);

        assertThatThrownBy(() -> chatService.read(10L, 1L, 5L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.INVALID_INPUT.getMessage());
    }

    @Test
    void 채팅방_참여자_검증_실패_테스트() {
        given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> chatService.validateParticipant(10L, 1L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.CHAT_ROOM_FORBIDDEN.getMessage());
    }

    @Test
    void 마지막_메시지_업데이트_성공_테스트() {
        given(chatRoomRepository.updateLastMessageIfNewer(eq(10L), eq("마지막"), any(LocalDateTime.class)))
                .willReturn(1);

        chatService.updateLastMessage(10L, "마지막", LocalDateTime.now());

        then(chatRoomRepository).should().updateLastMessageIfNewer(eq(10L), eq("마지막"), any(LocalDateTime.class));
    }

    @Test
    void 마지막_메시지_업데이트_실패_테스트_방_없음() {
        given(chatRoomRepository.updateLastMessageIfNewer(eq(10L), eq("마지막"), any(LocalDateTime.class)))
                .willReturn(0);
        given(chatRoomRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> chatService.updateLastMessage(10L, "마지막", LocalDateTime.now()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    void 채팅_히스토리_조회_성공_테스트_hasNext_true() {
        // given
        given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(true);

        RealtimeChatMessage m1 = RealtimeChatMessage.of(10L, 1L, "첫번째");
        ReflectionTestUtils.setField(m1, "id", 1L);

        RealtimeChatMessage m2 = RealtimeChatMessage.of(10L, 1L, "두번째");
        ReflectionTestUtils.setField(m2, "id", 2L);

        RealtimeChatMessage m3 = RealtimeChatMessage.of(10L, 1L, "세번째");
        ReflectionTestUtils.setField(m3, "id", 3L);

        // size=2 요청했을 때 3개 반환 → hasNext=true
        given(chatMessageRepository.findMessages(eq(10L), eq(null), any(PageRequest.class)))
                .willReturn(List.of(m3, m2, m1)); // id DESC 정렬

        // when
        var result = chatService.getChatHistory(10L, 1L, null, 2);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.responses()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(2L); // 잘린 리스트의 마지막 id
    }

    @Test
    void 채팅_히스토리_조회_성공_테스트_hasNext_false() {
        given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(true);

        RealtimeChatMessage m1 = RealtimeChatMessage.of(10L, 1L, "첫번째");
        ReflectionTestUtils.setField(m1, "id", 1L);

        RealtimeChatMessage m2 = RealtimeChatMessage.of(10L, 1L, "두번째");
        ReflectionTestUtils.setField(m2, "id", 2L);

        // size=2 요청했을 때 2개 반환 → hasNext=false
        given(chatMessageRepository.findMessages(eq(10L), eq(null), any(PageRequest.class)))
                .willReturn(List.of(m2, m1));

        var result = chatService.getChatHistory(10L, 1L, null, 2);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.responses()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(1L);
    }

    @Test
    void 채팅_히스토리_조회_실패_테스트_size_잘못된_값() {
        assertThatThrownBy(() -> chatService.getChatHistory(10L, 1L, null, 0))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.INVALID_INPUT.getMessage());

        assertThatThrownBy(() -> chatService.getChatHistory(10L, 1L, null, 101))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.INVALID_INPUT.getMessage());
    }

    @Test
    void 채팅_히스토리_조회_실패_테스트_참여자_아님() {
        given(chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> chatService.getChatHistory(10L, 1L, null, 10))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.CHAT_ROOM_FORBIDDEN.getMessage());
    }
}
