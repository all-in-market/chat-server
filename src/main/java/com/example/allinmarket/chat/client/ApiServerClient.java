package com.example.allinmarket.chat.client;

import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiServerClient {

    private final RestClient apiServerRestClient;

    // 주문 목록 조회
    public String getOrders(String token) {
        try {
            return apiServerRestClient.get()
                    .uri("/orders")
                    .header(ChatConsts.AUTHORIZATION, token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    // 주문 단건 조회
    public String getOrder(String token, Long orderId) {
        try {
            return apiServerRestClient.get()
                    .uri("/orders/{orderId}", orderId)
                    .header(ChatConsts.AUTHORIZATION, token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    // 상품 목록 조회
    public String getProducts(String token, String keyword) {
        try {
            return apiServerRestClient.get()
                    .uri(uri -> uri.path("/products")
                            .queryParam("keyword", keyword)
                            .build())
                    .header(ChatConsts.AUTHORIZATION, token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    // 상품 단건 조회
    public String getProduct(String token, Long productId) {
        try {
            return apiServerRestClient.get()
                    .uri("/products/{productId}", productId)
                    .header(ChatConsts.AUTHORIZATION, token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    // 반품 신청
    public String createRefund(String token, Long orderId, String reason, String description) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        try {
            return apiServerRestClient.post()
                    .uri("/orders/{orderId}/refunds", orderId)
                    .header(ChatConsts.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefundRequest(reason, description))
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    private record RefundRequest(String reason, String description) {}
}