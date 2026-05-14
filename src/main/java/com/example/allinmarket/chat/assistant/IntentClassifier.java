package com.example.allinmarket.chat.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface IntentClassifier {

    @SystemMessage("""
            사용자의 입력을 분류하세요.
            
            SMALL_TALK: 인사, 감사, 단순 대화, 잡답
            INQUIRY: 주문, 상품, 반품, 교환, 정책 등 서비스 관련 질문/요청
            TOOL: 주문 조회, 상품 검색, 환불, API 실행 요청
            
            기준:
            - "검색", "상품", "주문", "조회", "반품", "환불" → TOOL
            - "정책", "가능 여부", "규정" → POLICY
            - 나머지 → SMALL_TALK
            
            반드시 SMALL_TALK, POLICY, TOOL 중 하나만 반환하세요.
            """)
    String classify(@UserMessage String message);
}
