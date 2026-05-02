package com.example.allinmarket.chat.memory;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final JacksonChatMessageJsonCodec codec = new JacksonChatMessageJsonCodec();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(ChatConsts.MEMORY_KEY_PREFIX + memoryId);

        if(json == null) {
            return Collections.emptyList();
        }
        try {
            return codec.messagesFromJson(json);
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 역직렬화 실패 memoryId={}", memoryId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String json = codec.messagesToJson(messages);
            redisTemplate.opsForValue().set(ChatConsts.MEMORY_KEY_PREFIX + memoryId, json, ChatConsts.MEMORY_TTL);
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 직렬화 실패 memoryId={}", memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(ChatConsts.MEMORY_KEY_PREFIX + memoryId);
    }
}
