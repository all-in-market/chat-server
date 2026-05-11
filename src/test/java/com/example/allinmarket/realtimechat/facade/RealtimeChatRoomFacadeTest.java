package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatRoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RealtimeChatRoomFacadeTest {

    @Mock
    private RealtimeChatRoomService chatRoomService;

    @InjectMocks
    private RealtimeChatRoomFacade chatRoomFacade;

    // ──────────────────────────────────────────────────────────
    // 정상 케이스
    // ──────────────────────────────────────────────────────────

    @Test
    void BUYER가_자신의_buyerId로_방_생성_성공_테스트() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        UserPrincipal userPrincipal = new UserPrincipal(buyerId, RealtimeChatSenderType.BUYER, "dummy-token");

        RealtimeChatRoom room = RealtimeChatRoom.of(buyerId, sellerId);
        given(chatRoomService.getOrCreateRoom(buyerId, sellerId)).willReturn(room);

        // when
        Long roomId = chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId);

        // then
        assertThat(roomId).isEqualTo(room.getId());
        then(chatRoomService).should().getOrCreateRoom(buyerId, sellerId);
    }

    @Test
    void SELLER가_자신의_sellerId로_방_생성_성공_테스트() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        UserPrincipal userPrincipal = new UserPrincipal(sellerId, RealtimeChatSenderType.SELLER, "dummy-token");

        RealtimeChatRoom room = RealtimeChatRoom.of(buyerId, sellerId);
        given(chatRoomService.getOrCreateRoom(buyerId, sellerId)).willReturn(room);

        // when
        Long roomId = chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId);

        // then
        assertThat(roomId).isEqualTo(room.getId());
        then(chatRoomService).should().getOrCreateRoom(buyerId, sellerId);
    }

    // ──────────────────────────────────────────────────────────
    // 예외 케이스
    // ──────────────────────────────────────────────────────────

    @Test
    void buyerId와_sellerId가_같으면_INVALID_INPUT_예외_발생_테스트() {
        // given
        Long sameId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(sameId, RealtimeChatSenderType.BUYER, "dummy-token");

        // when & then
        assertThatThrownBy(() -> chatRoomFacade.createRoom(userPrincipal, sameId, sameId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.INVALID_INPUT));

        then(chatRoomService).shouldHaveNoInteractions();
    }

    @Test
    void BUYER인데_buyerId가_본인_아니면_CHAT_ROOM_FORBIDDEN_예외_발생_테스트() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long otherUserId = 3L;
        UserPrincipal userPrincipal = new UserPrincipal(otherUserId, RealtimeChatSenderType.BUYER, "dummy-token");

        // when & then
        assertThatThrownBy(() -> chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        then(chatRoomService).shouldHaveNoInteractions();
    }

    @Test
    void SELLER인데_sellerId가_본인_아니면_CHAT_ROOM_FORBIDDEN_예외_발생_테스트() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long otherUserId = 3L;
        UserPrincipal userPrincipal = new UserPrincipal(otherUserId, RealtimeChatSenderType.SELLER, "dummy-token");

        // when & then
        assertThatThrownBy(() -> chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        then(chatRoomService).shouldHaveNoInteractions();
    }

    @Test
    void userId가_buyerId도_sellerId도_아니면_CHAT_ROOM_FORBIDDEN_예외_발생_테스트() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long strangerUserId = 99L;
        UserPrincipal userPrincipal = new UserPrincipal(strangerUserId, RealtimeChatSenderType.BUYER, "dummy-token");

        // when & then
        assertThatThrownBy(() -> chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        then(chatRoomService).shouldHaveNoInteractions();
    }
}