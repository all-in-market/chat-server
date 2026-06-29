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

/**
 * 사용자와 AI가 나눈 대화 내용을 redis에 기록하고 다음 질문 때 다시 꺼내오는 역할
 */
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;

    // redis에 저장되어있던 이전 대화 내용을 가져옴
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(ChatConsts.MEMORY_KEY_PREFIX + memoryId);
        if (json == null) return Collections.emptyList();
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    // redis에 현재 대화 내용을 저장
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
