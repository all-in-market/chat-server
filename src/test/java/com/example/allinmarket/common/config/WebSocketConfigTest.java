package com.example.allinmarket.common.config;

import com.example.allinmarket.common.security.JwtChannelInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketConfigTest {

    @Test
    @DisplayName("WebSocketConfig Bean이 정상 등록된다")
    void websocket_config_created() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();

        context.registerBean(
                JwtChannelInterceptor.class,
                () -> org.mockito.Mockito.mock(JwtChannelInterceptor.class)
        );

        context.register(WebSocketConfig.class);

        context.refresh();

        WebSocketConfig bean = context.getBean(WebSocketConfig.class);

        assertThat(bean).isNotNull();
    }
}