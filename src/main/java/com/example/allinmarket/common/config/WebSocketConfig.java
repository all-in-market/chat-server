package com.example.allinmarket.common.config;

import com.example.allinmarket.common.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        // 메세지를 받을 때 : /sub으로 시작하는 경로를 구독하면 브로커가 메세지를 전달
        messageBrokerRegistry.enableSimpleBroker("/sub");

        // 메세지를 보낼 때 : /pub으로 시작하는 메세지만 컨트롤러로 라우팅 됨
        messageBrokerRegistry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        // 웹소켓 연결 엔드포인트 : ws://localhost:8080/chat
        stompEndpointRegistry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*"); // 테스트용 모든 도메인 허용

    }

    @Override // 인터셉터 등록 로직
    public void configureClientInboundChannel(ChannelRegistration channelRegistration) {
        log.info("인터셉터 등록 확인");
        channelRegistration.interceptors(jwtChannelInterceptor);
    }
}
