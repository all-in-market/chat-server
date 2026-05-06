package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(ChatConsts.MEMORY_KEY_PREFIX + memoryId);
        if (json == null) return Collections.emptyList();
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(ChatConsts.MEMORY_KEY_PREFIX + memoryId, json, ChatConsts.MEMORY_TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(ChatConsts.MEMORY_KEY_PREFIX + memoryId);
    }
}
