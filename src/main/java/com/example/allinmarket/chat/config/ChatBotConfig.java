package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.memory.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatBotConfig {

    private final RedisChatMemoryStore redisChatMemoryStore;

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> {
            String userId = memoryId.toString().split(":")[0];
            return MessageWindowChatMemory.builder()
                    .id(userId)
                    .maxMessages(10)
                    .chatMemoryStore(redisChatMemoryStore)
                    .build();
        };
    }
}
