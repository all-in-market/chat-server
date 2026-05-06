package com.example.allinmarket.chat.assistant;

import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AiAssistant {

    @SystemMessage("""
            당신은 멀티벤더 마켓 플랫폼의 친절한 AI 어시스턴트입니다.
            
            역할:
            - 반품/교환 정책 안내
            - 주문 조회 및 상품 검색 도움
            - 고객 문의 응대
            
            규칙:
            - 항상 한국어로 답변하세요.
            - 모르는 내용은 모른다고 솔직하게 말하세요.
            - 정책 관련 질문은 제공된 문서를 기반으로 답변하세요.
            - 반품은 사용자에게 반드시 확인 후 실행하세요.
            - 욕설, 비하, 혐오 표현이 포함된 메시지는 정중하게 거절하세요.
            - 부적절한 내용에는 답변하지 마세요.
            """)
    TokenStream chat(@MemoryId Long userId, @UserMessage String message);

    @SystemMessage("""
            당신은 친절한 AI 어시스턴트입니다.
            항상 한국어로 짧고 자연스럽게 답변하세요.
            욕설, 비하, 혐오 표현이 포함된 메시지는 정중하게 거절하세요.
            부적절한 내용에는 답변하지 마세요.
            """)
    TokenStream  smallTalk(@MemoryId Long userId, @UserMessage String message);
}
