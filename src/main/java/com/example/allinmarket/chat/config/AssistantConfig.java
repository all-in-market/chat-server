package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.assistant.PolicyAssistant;
import com.example.allinmarket.chat.assistant.SmallTalkAssistant;
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
            ContentRetriever contentRetriever
    ) {

        return AiServices.builder(PolicyAssistant.class)
                .streamingChatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }

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

