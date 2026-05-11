package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatMemoryStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisChatMemoryStore redisChatMemoryStore;

    // ────────────────────────────────────────────────────
    // opsForValue()는 각 테스트에서 필요할 때만 stub합니다.
    // @BeforeEach에 넣으면 opsForValue()를 쓰지 않는 테스트
    // (deleteMessages 등)에서 UnnecessaryStubbingException 발생.
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMessages")
    class GetMessagesTest {

        @Test
        @DisplayName("저장된 메시지가 없으면 빈 목록을 반환한다")
        void getMessages_저장_없음() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(ChatConsts.MEMORY_KEY_PREFIX + "user-1")).willReturn(null);

            List<ChatMessage> result = redisChatMemoryStore.getMessages("user-1");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("저장된 JSON이 있으면 역직렬화하여 반환한다")
        void getMessages_정상_조회() {
            List<ChatMessage> messages = List.of(
                    UserMessage.from("반품 신청하고 싶어요"),
                    AiMessage.from("네, 도와드리겠습니다.")
            );
            String json = dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson(messages);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(ChatConsts.MEMORY_KEY_PREFIX + "user-1")).willReturn(json);

            List<ChatMessage> result = redisChatMemoryStore.getMessages("user-1");

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(UserMessage.class);
            assertThat(result.get(1)).isInstanceOf(AiMessage.class);
        }

        @Test
        @DisplayName("memoryId는 접두사와 결합되어 Redis 키로 사용된다")
        void getMessages_키_형식_검증() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn(null);

            redisChatMemoryStore.getMessages(42L);

            verify(valueOperations).get(ChatConsts.MEMORY_KEY_PREFIX + "42");
        }
    }

    @Nested
    @DisplayName("updateMessages")
    class UpdateMessagesTest {

        @Test
        @DisplayName("메시지를 직렬화하여 TTL과 함께 저장한다")
        void updateMessages_저장_성공() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            List<ChatMessage> messages = List.of(
                    UserMessage.from("안녕하세요"),
                    AiMessage.from("안녕하세요!")
            );

            redisChatMemoryStore.updateMessages("user-1", messages);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    eq(ChatConsts.MEMORY_TTL)
            );

            assertThat(keyCaptor.getValue()).isEqualTo(ChatConsts.MEMORY_KEY_PREFIX + "user-1");
            assertThat(valueCaptor.getValue()).contains("안녕하세요");
        }

        @Test
        @DisplayName("빈 메시지 목록도 직렬화하여 저장한다")
        void updateMessages_빈_목록_저장() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            redisChatMemoryStore.updateMessages("user-1", List.of());

            verify(valueOperations).set(
                    eq(ChatConsts.MEMORY_KEY_PREFIX + "user-1"),
                    anyString(),
                    eq(ChatConsts.MEMORY_TTL)
            );
        }

        @Test
        @DisplayName("저장 시 TTL이 항상 MEMORY_TTL로 설정된다")
        void updateMessages_TTL_검증() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            redisChatMemoryStore.updateMessages("user-99", List.of(UserMessage.from("테스트")));

            ArgumentCaptor<java.time.Duration> ttlCaptor =
                    ArgumentCaptor.forClass(java.time.Duration.class);
            verify(valueOperations).set(anyString(), anyString(), ttlCaptor.capture());

            assertThat(ttlCaptor.getValue()).isEqualTo(ChatConsts.MEMORY_TTL);
        }
    }

    @Nested
    @DisplayName("deleteMessages")
    class DeleteMessagesTest {

        @Test
        @DisplayName("memoryId에 해당하는 Redis 키를 삭제한다")
        void deleteMessages_삭제_성공() {
            redisChatMemoryStore.deleteMessages("user-1");

            verify(redisTemplate).delete(ChatConsts.MEMORY_KEY_PREFIX + "user-1");
        }

        @Test
        @DisplayName("Long 타입 memoryId도 문자열로 변환하여 올바른 키를 삭제한다")
        void deleteMessages_Long_memoryId() {
            redisChatMemoryStore.deleteMessages(123L);

            verify(redisTemplate).delete(ChatConsts.MEMORY_KEY_PREFIX + "123");
        }
    }
}