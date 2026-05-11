package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.enums.RealtimeChatMessageType;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatProvider;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import com.example.allinmarket.realtimechat.service.RedisUnreadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * RealtimeChatFacade 단위 테스트
 *
 * afterCommit 콜백은 TransactionSynchronizationManager에 등록만 하고 실제로는 실행되지 않으므로,
 * handleMessage의 분기·예외 경로만 검증한다.
 * afterCommit 블록 내부 동작(unread 증가, Redis 발행 등)은 RealtimeChatFacadeIntegrationTest에서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RealtimeChatFacadeTest {

    @Mock private RealtimeChatService chatService;
    @Mock private RedisUnreadService unreadService;
    @Mock private RedisPublisher redisPublisher;
    @Mock private RealtimeChatProvider chatProvider;
    @Mock private SimpMessageSendingOperations messageSendingOperations;

    @InjectMocks
    private RealtimeChatFacade chatFacade;

    // ──────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────

    private UserPrincipal buyer(Long userId) {
        return new UserPrincipal(userId, RealtimeChatSenderType.BUYER, "token");
    }

    private RealtimeChatMessageDto dto(RealtimeChatMessageType type) {
        return new RealtimeChatMessageDto(type, 10L, null, "hi", Map.of(), "temp-1");
    }

    private RealtimeChatMessageDto dtoWithNullType() {
        return new RealtimeChatMessageDto(null, 10L, null, "hi", Map.of(), "temp-1");
    }

    // TransactionSynchronizationManager 초기화 (단위 테스트 환경에서 직접 트랜잭션 없이 호출 시 필요)
    private void initTxManager() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private void clearTxManager() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ──────────────────────────────────────────────────────────
    // handleMessage – 정상: ENTER 분기
    // ──────────────────────────────────────────────────────────

    @Test
    void handleMessage_ENTER_타입이면_enterRoom_호출_테스트() {
        // given
        initTxManager();
        try {
            UserPrincipal principal = buyer(1L);
            RealtimeChatMessageDto dto = dto(RealtimeChatMessageType.ENTER);

            willDoNothing().given(chatService).validateParticipant(10L, 1L);
            given(chatProvider.getUserName(1L, RealtimeChatSenderType.BUYER)).willReturn("구매자");

            // when
            chatFacade.handleMessage(dto, principal);

            // then: validateParticipant 두 번 호출 (handleMessage + enterRoom 각 1회)
            then(chatService).should(times(2)).validateParticipant(10L, 1L);
        } finally {
            clearTxManager();
        }
    }

    // ──────────────────────────────────────────────────────────
    // handleMessage – 정상: TALK 분기
    // ──────────────────────────────────────────────────────────

    @Test
    void handleMessage_TALK_타입이면_sendMessage_호출_테스트() {
        // given
        initTxManager();
        try {
            UserPrincipal principal = buyer(1L);
            RealtimeChatMessageDto dto = dto(RealtimeChatMessageType.TALK);

            willDoNothing().given(chatService).validateParticipant(10L, 1L);

            com.example.allinmarket.realtimechat.entity.RealtimeChatMessage saved =
                    org.mockito.Mockito.mock(com.example.allinmarket.realtimechat.entity.RealtimeChatMessage.class);
            given(saved.getRoomId()).willReturn(10L);
            given(saved.getMessage()).willReturn("hi");
            given(saved.getCreatedAt()).willReturn(java.time.LocalDateTime.now());
            given(chatService.save(any(), eq(1L))).willReturn(saved);
            given(chatService.getParticipantIds(10L)).willReturn(java.util.List.of(1L, 2L));
            given(chatProvider.getUserName(1L, RealtimeChatSenderType.BUYER)).willReturn("구매자");

            // when
            chatFacade.handleMessage(dto, principal);

            // then
            then(chatService).should(times(2)).validateParticipant(10L, 1L);
            then(chatService).should().save(any(), eq(1L));
        } finally {
            clearTxManager();
        }
    }

    // ──────────────────────────────────────────────────────────
    // handleMessage – 예외: type == null
    // ──────────────────────────────────────────────────────────

    @Test
    void handleMessage_타입이_null이면_MESSAGE_TYPE_INVALID_예외_발생_테스트() {
        // given
        initTxManager();
        try {
            UserPrincipal principal = buyer(1L);
            RealtimeChatMessageDto dto = dtoWithNullType();

            willDoNothing().given(chatService).validateParticipant(10L, 1L);

            // when & then
            assertThatThrownBy(() -> chatFacade.handleMessage(dto, principal))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> org.assertj.core.api.Assertions
                            .assertThat(((BaseException) e).getErrorEnum())
                            .isEqualTo(ErrorEnum.MESSAGE_TYPE_INVALID));

            // ACK 전송 확인
            then(messageSendingOperations).should().convertAndSendToUser(
                    eq("1"), eq("/sub/chat/status"), any()
            );
        } finally {
            clearTxManager();
        }
    }

    // ──────────────────────────────────────────────────────────
    // handleMessage – 예외: validateParticipant에서 BaseException
    // ──────────────────────────────────────────────────────────

    @Test
    void handleMessage_참여자_검증_실패시_BaseException_재던지고_ACK_전송_테스트() {
        // given
        initTxManager();
        try {
            UserPrincipal principal = buyer(1L);
            RealtimeChatMessageDto dto = dto(RealtimeChatMessageType.TALK);

            willThrow(new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN))
                    .given(chatService).validateParticipant(10L, 1L);

            // when & then
            assertThatThrownBy(() -> chatFacade.handleMessage(dto, principal))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> org.assertj.core.api.Assertions
                            .assertThat(((BaseException) e).getErrorEnum())
                            .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN));

            then(messageSendingOperations).should().convertAndSendToUser(
                    eq("1"), eq("/sub/chat/status"), any()
            );
        } finally {
            clearTxManager();
        }
    }

    // ──────────────────────────────────────────────────────────
    // handleMessage – 예외: 예상치 못한 RuntimeException
    // ──────────────────────────────────────────────────────────

    @Test
    void handleMessage_시스템_예외_발생시_재던지고_SYSTEM_ERROR_ACK_전송_테스트() {
        // given
        initTxManager();
        try {
            UserPrincipal principal = buyer(1L);
            RealtimeChatMessageDto dto = dto(RealtimeChatMessageType.TALK);

            willThrow(new RuntimeException("DB 연결 실패"))
                    .given(chatService).validateParticipant(10L, 1L);

            // when & then
            assertThatThrownBy(() -> chatFacade.handleMessage(dto, principal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");

            then(messageSendingOperations).should().convertAndSendToUser(
                    eq("1"),
                    eq("/sub/chat/status"),
                    argThat(payload -> payload.toString().contains("SYSTEM_ERROR"))
            );
        } finally {
            clearTxManager();
        }
    }
}