package com.example.allinmarket.chat.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatBotConfig {

    private final RedisChatMemoryStore redisChatMemoryStore;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    @Bean
    public TaskExecutor chatTaskExecutor() {
        log.info("가상 스레드 기반 TaskExecutor 초기화");

        return new TaskExecutor() {
            @Override
            public void execute(Runnable task) {
                SecurityContext parent = SecurityContextHolder.getContext();
                virtualThreadExecutor.execute(() -> {
                    try {
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(parent.getAuthentication());
                        SecurityContextHolder.setContext(context);
                        task.run();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
            }
        };
    }
    @PreDestroy
    public void shutdownVirtualThreadExecutor() {
        virtualThreadExecutor.shutdown();
    }
}
