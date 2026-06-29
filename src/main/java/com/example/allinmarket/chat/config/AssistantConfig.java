package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.assistant.PolicyAssistant;
import com.example.allinmarket.chat.assistant.SmallTalkAssistant;
import com.example.allinmarket.chat.tool.ChatTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {

    @Bean
    public PolicyAssistant policyAssistant(
            StreamingChatModel chatModel,
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever,
            ChatTools chatTools
    ) {

        // AiServices로 만든 PolicyAssistant 구현체에는 자동으로 chatMemoryProvider를 통해 이전 대화 내용을 가져오는 코드가 추가됨
        return AiServices.builder(PolicyAssistant.class)
                .streamingChatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .tools(chatTools)
                .build();
    }

    // 스몰토크 기반이라 RAG용 contentRetriever가 없음
    @Bean
    public SmallTalkAssistant smallTalkAssistant(
            StreamingChatModel chatModel,
            ChatMemoryProvider chatMemoryProvider
    ) {

        return AiServices.builder(SmallTalkAssistant.class)
                .streamingChatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    @Bean
    public IntentClassifier intentClassifier(ChatModel chatModel) {
        return AiServices.builder(IntentClassifier.class)
                .chatModel(chatModel)
                .build();
    }
}

