package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "OPENAI_API_KEY=test-key")
@ActiveProfiles("test")
class ChatConfigIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    // ────────────────────────────────────────────────────
    // ChatBotConfig
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ChatBotConfig")
    class ChatBotConfigTest {

        @Autowired
        private ChatMemoryProvider chatMemoryProvider;

        @Autowired
        private TaskExecutor chatTaskExecutor;

        @Test
        @DisplayName("ChatMemoryProvider 빈이 생성된다")
        void chatMemoryProvider_빈_생성() {
            assertThat(chatMemoryProvider).isNotNull();
        }

        @Test
        @DisplayName("ChatMemoryProvider가 memoryId별로 독립적인 메모리를 반환한다")
        void chatMemoryProvider_독립_메모리() {
            var mem1 = chatMemoryProvider.get(1L);
            var mem2 = chatMemoryProvider.get(2L);

            assertThat(mem1).isNotNull();
            assertThat(mem2).isNotNull();
            assertThat(mem1).isNotSameAs(mem2);
        }

        @Test
        @DisplayName("chatTaskExecutor 빈이 생성된다")
        void chatTaskExecutor_빈_생성() {
            assertThat(chatTaskExecutor).isNotNull();
        }

        @Test
        @DisplayName("chatTaskExecutor가 Runnable을 실행한다")
        void chatTaskExecutor_Runnable_실행() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);

            chatTaskExecutor.execute(latch::countDown);

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        @Test
        @DisplayName("chatTaskExecutor가 부모 SecurityContext를 자식 스레드에 전파한다")
        void chatTaskExecutor_SecurityContext_전파() throws InterruptedException {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "test-user", null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            AtomicReference<String> capturedPrincipal = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            chatTaskExecutor.execute(() -> {
                var context = SecurityContextHolder.getContext();
                if (context.getAuthentication() != null) {
                    capturedPrincipal.set(context.getAuthentication().getName());
                }
                latch.countDown();
            });

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(capturedPrincipal.get()).isEqualTo("test-user");

            SecurityContextHolder.clearContext();
        }
    }

    // ────────────────────────────────────────────────────
    // RestClientConfig
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("RestClientConfig")
    class RestClientConfigTest {

        @Test
        @DisplayName("apiServerRestClient 빈이 생성된다")
        void apiServerRestClient_빈_생성() {
            var restClient = applicationContext.getBean(
                    "apiServerRestClient", org.springframework.web.client.RestClient.class);
            assertThat(restClient).isNotNull();
        }
    }

    // ────────────────────────────────────────────────────
    // ChatConsts 상수 검증 (Spring 컨텍스트 불필요하지만 통합 테스트에 포함)
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ChatConsts")
    class ChatConstsTest {

        @Test
        @DisplayName("MEMORY_TTL이 양수이다")
        void memoryTtl_양수() {
            assertThat(ChatConsts.MEMORY_TTL).isNotNull();
            assertThat(ChatConsts.MEMORY_TTL.isNegative()).isFalse();
            assertThat(ChatConsts.MEMORY_TTL.isZero()).isFalse();
        }

        @Test
        @DisplayName("MEMORY_KEY_PREFIX가 비어있지 않다")
        void memoryKeyPrefix_비어있지_않음() {
            assertThat(ChatConsts.MEMORY_KEY_PREFIX).isNotBlank();
        }

        @Test
        @DisplayName("SSE_TIMEOUT이 0보다 크다")
        void sseTimeout_양수() {
            assertThat(ChatConsts.SSE_TIMEOUT).isPositive();
        }

        @Test
        @DisplayName("RRF_K가 양수이다")
        void rrfK_양수() {
            assertThat(ChatConsts.RRF_K).isPositive();
        }

        @Test
        @DisplayName("HYBRID_FINAL_SIZE가 양수이다")
        void hybridFinalSize_양수() {
            assertThat(ChatConsts.HYBRID_FINAL_SIZE).isPositive();
        }

        @Test
        @DisplayName("HYBRID_CANDIDATE_SIZE가 HYBRID_FINAL_SIZE 이상이다")
        void hybridCandidateSize_finalSize_이상() {
            assertThat(ChatConsts.HYBRID_CANDIDATE_SIZE)
                    .isGreaterThanOrEqualTo(ChatConsts.HYBRID_FINAL_SIZE);
        }
    }
}