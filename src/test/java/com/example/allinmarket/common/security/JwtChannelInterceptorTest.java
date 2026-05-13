package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    JwtProvider jwtProvider;

    @Mock
    MessageChannel channel;

    @InjectMocks
    JwtChannelInterceptor jwtChannelInterceptor;

    private static final String VALID_TOKEN = "valid.jwt.token";

    // ── 헬퍼: STOMP 메시지 생성 ────────────────────────────────────────────────

    private Message<?> buildMessage(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildNonConnectMessage(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ── CONNECT 명령 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("CONNECT - 유효한 토큰 → UserPrincipal 세팅 후 메시지 반환")
    void connect_validToken_setsPrincipal() {
        // given
        when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtProvider.getUserId(VALID_TOKEN)).thenReturn(1L);
        when(jwtProvider.getSenderType(VALID_TOKEN)).thenReturn(RealtimeChatSenderType.BUYER);

        Message<?> message = buildMessage(StompCommand.CONNECT, "Bearer " + VALID_TOKEN);

        // when
        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        // then
        assertThat(result).isNotNull();

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.senderType()).isEqualTo(RealtimeChatSenderType.BUYER);
        assertThat(principal.token()).isEqualTo(VALID_TOKEN);
    }

    @Test
    @DisplayName("CONNECT - Authorization 헤더 없음 → BaseException(UNAUTHORIZED)")
    void connect_noAuthorizationHeader_throwsUnauthorized() {
        Message<?> message = buildMessage(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    @Test
    @DisplayName("CONNECT - Bearer 접두사 없음 → BaseException(UNAUTHORIZED)")
    void connect_noBearerPrefix_throwsUnauthorized() {
        Message<?> message = buildMessage(StompCommand.CONNECT, VALID_TOKEN);

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    @Test
    @DisplayName("CONNECT - 유효하지 않은 토큰 → BaseException(UNAUTHORIZED)")
    void connect_invalidToken_throwsUnauthorized() {
        String invalidToken = "invalid.token";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        Message<?> message = buildMessage(StompCommand.CONNECT, "Bearer " + invalidToken);

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    // ── CONNECT 외 명령 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("SEND 명령 → 토큰 검증 없이 메시지 그대로 반환")
    void nonConnect_send_passesThrough() {
        Message<?> message = buildNonConnectMessage(StompCommand.SEND);

        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("SUBSCRIBE 명령 → 토큰 검증 없이 메시지 그대로 반환")
    void nonConnect_subscribe_passesThrough() {
        Message<?> message = buildNonConnectMessage(StompCommand.SUBSCRIBE);

        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(jwtProvider);
    }
}