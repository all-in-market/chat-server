package com.example.allinmarket.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    @DisplayName("dashboardExecutor Bean 등록 테스트")
    void dashboardExecutor_등록() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AsyncConfig.class);

        Executor executor = context.getBean("dashboardExecutor", Executor.class);

        assertThat(executor).isNotNull();

        context.close();
    }
}