package com.example.allinmarket.chat.tool;

import com.example.allinmarket.chat.client.ApiServerClient;
import com.example.allinmarket.chat.security.TokenStore;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatTools {

    private final ApiServerClient apiServerClient;
    private final TokenStore tokenStore;

    @Tool("사용자의 주문 목록을 조회합니다.")
    public String getOrders(@ToolMemoryId Long userId) {
        return apiServerClient.getOrders(tokenStore.get(userId));
    }

    @Tool("특정 주문의 상세 정보를 조회합니다.")
    public String getOrder(@ToolMemoryId Long userId, Long orderId) {
        return apiServerClient.getOrder(tokenStore.get(userId), orderId);
    }

    @Tool("키워드로 상품을 검색합니다.")
    public String searchProducts(@ToolMemoryId Long userId, String keyword) {
        return apiServerClient.getProducts(tokenStore.get(userId), keyword);
    }

    @Tool("특정 상품의 상세 정보를 조회합니다.")
    public String getProduct(@ToolMemoryId Long userId, Long productId) {
        return apiServerClient.getProduct(tokenStore.get(userId), productId);
    }

    @Tool("""
            반품을 신청합니다. 사용자에게 반드시 확인 후 실행하세요.
            reason 가능한 값:
            - CHANGE_OF_MIND: 고객 단순 변심
            - WRONG_ITEM: 잘못된 상품
            - DAMAGED: 상품 손상
            - PAYMENT_AMOUNT_MISMATCH: 주문 금액과 실결제 금액이 상이
            description은 추가 설명 (선택사항)
            """)
    public String createRefund(@ToolMemoryId Long userId, Long orderId, String reason, String description) {
        return apiServerClient.createRefund(tokenStore.get(userId), orderId, reason, description);
    }
}
