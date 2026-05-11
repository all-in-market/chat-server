package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.enums.RealtimeChatMessageType;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatProvider;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import com.example.allinmarket.realtimechat.service.RedisUnreadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * RealtimeChatFacade 통합 테스트
 *
 * @Transactional + afterCommit 콜백이 실제로 실행되는 환경에서
 * sendMessage / enterRoom 의 afterCommit 블록 내부 동작을 검증한다.
 *
 * - unread 증가 / 초기화
 * - Redis publish
 * - ACK 전송
 */
@SpringBootTest
@ActiveProfiles("test")
class RealtimeChatFacadeIntegrationTest {

    @Autowired
    private RealtimeChatFacade chatFacade;

    @MockitoBean private RealtimeChatService chatService;
    @MockitoBean private RedisUnreadService unreadService;
    @MockitoBean private RedisPublisher redisPublisher;
    @MockitoBean private RealtimeChatProvider chatProvider;
    @MockitoBean private SimpMessageSendingOperations messageSendingOperations;

    private static final Long ROOM_ID = 10L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    // ──────────────────────────────────────────────────────────
    // sendMessage afterCommit
    // ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_afterCommit_unread_증가_및_Redis_발행_성공_테스트() {
        // given
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.TALK, ROOM_ID, null, "안녕", Map.of(), "temp-1"
        );

        RealtimeChatMessage saved = mock(RealtimeChatMessage.class);
        given(saved.getRoomId()).willReturn(ROOM_ID);
        given(saved.getMessage()).willReturn("안녕");
        given(saved.getCreatedAt()).willReturn(LocalDateTime.now());

        willDoNothing().given(chatService).validateParticipant(ROOM_ID, BUYER_ID);
        given(chatService.save(any(), eq(BUYER_ID))).willReturn(saved);
        willDoNothing().given(chatService).updateLastMessage(any(), any(), any());
        given(chatService.getParticipantIds(ROOM_ID)).willReturn(List.of(BUYER_ID, SELLER_ID));
        given(chatProvider.getUserName(BUYER_ID, RealtimeChatSenderType.BUYER)).willReturn("구매자");
        given(unreadService.getUnread(eq(ROOM_ID), any())).willReturn(1);

        // when
        chatFacade.sendMessage(dto, BUYER_ID, RealtimeChatSenderType.BUYER);

        // then
        // 발신자(BUYER_ID) 제외한 SELLER_ID의 unread 증가
        then(unreadService).should().incrementUnread(ROOM_ID, SELLER_ID);
        then(unreadService).should(never()).incrementUnread(ROOM_ID, BUYER_ID);

        // Redis 발행
        then(redisPublisher).should().publish(eq(ROOM_ID), any());

        // SUCCESS ACK 전송
        then(messageSendingOperations).should().convertAndSendToUser(
                eq(BUYER_ID.toString()),
                eq("/sub/chat/status"),
                argThat(payload -> payload.toString().contains("SUCCESS"))
        );
    }

    // ──────────────────────────────────────────────────────────
    // enterRoom afterCommit
    // ──────────────────────────────────────────────────────────

    @Test
    void enterRoom_afterCommit_unread_초기화_및_Redis_발행_성공_테스트() {
        // given
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.ENTER, ROOM_ID, null, null, Map.of(), "temp-2"
        );

        willDoNothing().given(chatService).validateParticipant(ROOM_ID, BUYER_ID);
        given(chatProvider.getUserName(BUYER_ID, RealtimeChatSenderType.BUYER)).willReturn("구매자");

        // when
        chatFacade.enterRoom(dto, BUYER_ID, RealtimeChatSenderType.BUYER);

        // then
        then(unreadService).should().resetUnread(ROOM_ID, BUYER_ID);
        then(redisPublisher).should().publish(eq(ROOM_ID), any());
        then(messageSendingOperations).should().convertAndSendToUser(
                eq(BUYER_ID.toString()),
                eq("/sub/chat/status"),
                argThat(payload -> payload.toString().contains("SUCCESS"))
        );
    }

    // ──────────────────────────────────────────────────────────
    // sendMessage afterCommit – Redis 발행 실패 시 REDIS_PUBLISH_FAILED ACK
    // ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_Redis_발행_실패시_REDIS_PUBLISH_FAILED_ACK_전송_테스트() {
        // given
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.TALK, ROOM_ID, null, "안녕", Map.of(), "temp-3"
        );

        RealtimeChatMessage saved = mock(RealtimeChatMessage.class);
        given(saved.getRoomId()).willReturn(ROOM_ID);
        given(saved.getMessage()).willReturn("안녕");
        given(saved.getCreatedAt()).willReturn(LocalDateTime.now());

        willDoNothing().given(chatService).validateParticipant(ROOM_ID, BUYER_ID);
        given(chatService.save(any(), eq(BUYER_ID))).willReturn(saved);
        willDoNothing().given(chatService).updateLastMessage(any(), any(), any());
        given(chatService.getParticipantIds(ROOM_ID)).willReturn(List.of(BUYER_ID, SELLER_ID));
        given(chatProvider.getUserName(BUYER_ID, RealtimeChatSenderType.BUYER)).willReturn("구매자");
        given(unreadService.getUnread(eq(ROOM_ID), any())).willReturn(0);

        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(redisPublisher).publish(eq(ROOM_ID), any());

        // when
        chatFacade.sendMessage(dto, BUYER_ID, RealtimeChatSenderType.BUYER);

        // then
        // REDIS_PUBLISH_FAILED ACK 먼저, 이후 SUCCESS ACK
        then(messageSendingOperations).should(atLeastOnce()).convertAndSendToUser(
                eq(BUYER_ID.toString()),
                eq("/sub/chat/status"),
                argThat(payload -> payload.toString().contains("REDIS_PUBLISH_FAILED"))
        );
    }

    // ──────────────────────────────────────────────────────────
    // sendMessage – tempId가 null이면 ACK 전송 없음
    // ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_tempId가_null이면_ACK_미전송_테스트() {
        // given
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.TALK, ROOM_ID, null, "안녕", Map.of(), null // tempId=null
        );

        RealtimeChatMessage saved = mock(RealtimeChatMessage.class);
        given(saved.getRoomId()).willReturn(ROOM_ID);
        given(saved.getMessage()).willReturn("안녕");
        given(saved.getCreatedAt()).willReturn(LocalDateTime.now());

        willDoNothing().given(chatService).validateParticipant(ROOM_ID, BUYER_ID);
        given(chatService.save(any(), eq(BUYER_ID))).willReturn(saved);
        willDoNothing().given(chatService).updateLastMessage(any(), any(), any());
        given(chatService.getParticipantIds(ROOM_ID)).willReturn(List.of(BUYER_ID, SELLER_ID));
        given(chatProvider.getUserName(BUYER_ID, RealtimeChatSenderType.BUYER)).willReturn("구매자");
        given(unreadService.getUnread(eq(ROOM_ID), any())).willReturn(0);

        // when
        chatFacade.sendMessage(dto, BUYER_ID, RealtimeChatSenderType.BUYER);

        // then
        then(messageSendingOperations).shouldHaveNoInteractions();
    }
}