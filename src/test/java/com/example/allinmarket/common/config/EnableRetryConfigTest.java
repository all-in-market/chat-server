package com.example.allinmarket.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class EnableRetryConfigTest {

    @Test
    @DisplayName("EnableRetryConfig 로딩 성공")
    void enableRetryConfig_로드() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(
                        EnableRetryConfig.class
                );

        EnableRetryConfig bean =
                context.getBean(EnableRetryConfig.class);

        assertThat(bean).isNotNull();
    }
}