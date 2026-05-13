package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.assistant.PolicyAssistant;
import com.example.allinmarket.chat.assistant.SmallTalkAssistant;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssistantConfigTest {

    private final AssistantConfig assistantConfig =
            new AssistantConfig();

    @Nested
    @DisplayName("PolicyAssistant")
    class PolicyAssistantTest {

        @Test
        @DisplayName("PolicyAssistant 생성 성공")
        void policyAssistant_생성() {

            StreamingChatModel chatModel =
                    mock(StreamingChatModel.class);

            ChatMemoryProvider chatMemoryProvider =
                    mock(ChatMemoryProvider.class);

            ContentRetriever contentRetriever =
                    mock(ContentRetriever.class);

            PolicyAssistant assistant =
                    assistantConfig.policyAssistant(
                            chatModel,
                            chatMemoryProvider,
                            contentRetriever
                    );

            assertThat(assistant).isNotNull();
        }
    }

    @Nested
    @DisplayName("SmallTalkAssistant")
    class SmallTalkAssistantTest {

        @Test
        @DisplayName("SmallTalkAssistant 생성 성공")
        void smallTalkAssistant_생성() {

            StreamingChatModel chatModel =
                    mock(StreamingChatModel.class);

            ChatMemoryProvider chatMemoryProvider =
                    mock(ChatMemoryProvider.class);

            SmallTalkAssistant assistant =
                    assistantConfig.smallTalkAssistant(
                            chatModel,
                            chatMemoryProvider
                    );

            assertThat(assistant).isNotNull();
        }
    }

    @Nested
    @DisplayName("IntentClassifier")
    class IntentClassifierTest {

        @Test
        @DisplayName("IntentClassifier 생성 성공")
        void intentClassifier_생성() {

            ChatModel chatModel =
                    mock(ChatModel.class);

            IntentClassifier classifier =
                    assistantConfig.intentClassifier(chatModel);

            assertThat(classifier).isNotNull();
        }
    }
}