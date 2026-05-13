package com.example.allinmarket.chat.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatBotConfigTest {

    @Nested
    @DisplayName("chatMemoryProvider")
    class ChatMemoryProviderTest {

        @Test
        @DisplayName("ChatMemoryProvider 생성 성공")
        void chatMemoryProvider_생성() {

            RedisChatMemoryStore memoryStore =
                    mock(RedisChatMemoryStore.class);

            ChatBotConfig config =
                    new ChatBotConfig(memoryStore);

            ChatMemoryProvider provider =
                    config.chatMemoryProvider();

            assertThat(provider).isNotNull();
        }
    }

    @Nested
    @DisplayName("chatTaskExecutor")
    class ChatTaskExecutorTest {

        @Test
        @DisplayName("TaskExecutor 생성 성공")
        void taskExecutor_생성() {

            RedisChatMemoryStore memoryStore =
                    mock(RedisChatMemoryStore.class);

            ChatBotConfig config =
                    new ChatBotConfig(memoryStore);

            TaskExecutor executor =
                    config.chatTaskExecutor();

            assertThat(executor).isNotNull();
        }

        @Test
        @DisplayName("Virtual Thread에서도 SecurityContext 유지")
        void virtualThread_보안컨텍스트_유지() throws Exception {

            RedisChatMemoryStore memoryStore =
                    mock(RedisChatMemoryStore.class);

            ChatBotConfig config =
                    new ChatBotConfig(memoryStore);

            TaskExecutor executor =
                    config.chatTaskExecutor();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "user",
                            "password"
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(auth);

            CountDownLatch latch =
                    new CountDownLatch(1);

            AtomicReference<Object> principalRef =
                    new AtomicReference<>();

            executor.execute(() -> {
                try {

                    Object principal =
                            SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getPrincipal();

                    principalRef.set(principal);

                } finally {
                    latch.countDown();
                }
            });

            boolean completed =
                    latch.await(5, TimeUnit.SECONDS);

            assertThat(completed).isTrue();

            assertThat(principalRef.get())
                    .isEqualTo("user");
        }
    }
}