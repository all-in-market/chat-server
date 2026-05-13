package com.example.allinmarket.chat.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface PolicyAssistant {

    @SystemMessage("""
            당신은 멀티벤더 마켓 플랫폼의 AI 고객 어시스턴트입니다.

            주요 책임:
            - 반품/교환 정책 정확한 안내
            - 정책 문서 기반 질문 응답
            - 고객 문의 친절한 응대

            응답 원칙:
            1. 제공된 정책 문서 내용만으로 답변하세요.
            2. 문서에 없는 내용은 추측하지 마세요.
            3. 문서 부재 시 "해당 내용은 정책에 없습니다"라고 답변하세요.
            4. 항상 한국어로 간결하게 답변하세요.
            """)
    TokenStream chat(
            @MemoryId String memoryId,
            @UserMessage String message
    );
}

