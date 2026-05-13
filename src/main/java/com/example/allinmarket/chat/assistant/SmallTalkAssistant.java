package com.example.allinmarket.chat.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface SmallTalkAssistant {

    @SystemMessage("""
            당신은 친절한 AI 어시스턴트입니다.

            규칙:
            - 자연스럽고 짧게 대화하세요.
            - 사용자가 정책 질문을 하기 전까지 정책 내용을 언급하지 마세요.
            - 주문, 상품, 반품 정책을 먼저 설명하지 마세요.
            - 항상 한국어로 답변하세요.
            - 짧고 자연스럽게 응답하세요.
            """)
    TokenStream chat(
            @MemoryId String memoryId,
            @UserMessage String message
    );
}

