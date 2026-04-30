package com.example.allinmarket.chat.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface IntentClassifier {

    @SystemMessage("""
            사용자의 입력을 분류하세요.
            
            SMALL_TALK: 인사, 감사, 단순 대화, 잡답
            INQUIRY: 주문, 상품, 반품, 교환, 정책 등 서비스 관련 질문/요청
            
            반드시 SMALL_TALK 또는 INQUIRY 중 하나만 반환하세요.
            다른 텍스트는 절대 포함하지 마세요.
            """)
    String classify(@UserMessage String message);
}
