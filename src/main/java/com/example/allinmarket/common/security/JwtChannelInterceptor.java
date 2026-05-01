package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 웹소켓 연결 시(CONNECT) 토큰 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                throw new BaseException(ErrorEnum.UNAUTHORIZED);
            }

            token = token.substring(7);

            // 토큰 유효성 검사 (실패 시 예외 발생, 연결 거부)
            if (!jwtProvider.validateToken(token)) {
                throw new BaseException(ErrorEnum.UNAUTHORIZED);
            }

            Long userId = jwtProvider.getUserId(token);
            RealtimeChatSenderType senderType = jwtProvider.getSenderType(token);

            // Principal 세팅
            UserPrincipal principal = new UserPrincipal(userId, senderType);

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal, null, List.of()
            ));
        }

        return message;
    }
}
