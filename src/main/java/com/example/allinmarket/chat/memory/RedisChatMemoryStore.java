package com.example.allinmarket.chat.memory;

import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
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

        try {
        String json = redisTemplate.opsForValue().get(keyOf(memoryId));

        if(json == null) {
            return Collections.emptyList();
        }
            return codec.messagesFromJson(json);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 역직렬화 실패 memoryId={}", memoryId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String json = codec.messagesToJson(messages);
            redisTemplate.opsForValue()
                    .set(keyOf(memoryId), json, ChatConsts.MEMORY_TTL);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 저장 실패", e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            redisTemplate.delete(keyOf(memoryId));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 삭제 실패", e);
        }
    }

    private String keyOf(Object memoryId) {
        String id = (memoryId == null) ? "" : memoryId.toString().trim();
        if (id.isEmpty()) {
            throw new BaseException(ErrorEnum.CHAT_MEMORY_ID_INVALID);
        }
        return ChatConsts.MEMORY_KEY_PREFIX + id;
    }
}
