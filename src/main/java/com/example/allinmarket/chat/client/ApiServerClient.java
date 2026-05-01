package com.example.allinmarket.chat.client;

import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ApiServerClient {

    private final WebClient apiServerWebClient;

    // 주문 목록 조회
    public String getOrders(String token) {
        return apiServerWebClient.get()
                .uri("/orders")
                .header(ChatConsts.AUTHORIZATION, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INVALID_INPUT))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    // 주문 단건 조회
    public String getOrder(String token, Long orderId) {
        return apiServerWebClient.get()
                .uri("/orders/{orderId}", orderId)
                .header(ChatConsts.AUTHORIZATION, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INVALID_INPUT))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    // 상품 검색
    public String getProducts(String token, String keyword) {
        return apiServerWebClient.get()
                .uri(uri -> uri.path("/products")
                        .queryParam("keyword", keyword)
                        .build())
                .header(ChatConsts.AUTHORIZATION, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INVALID_INPUT))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    // 상품 단건 조회
    public String getProduct(String token, Long productId) {
        return apiServerWebClient.get()
                .uri("/products/{productId}", productId)
                .header(ChatConsts.AUTHORIZATION, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INVALID_INPUT))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    // 반품 신청
    public String createRefund(String token, Long orderId, String reason, String description) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        return apiServerWebClient.post()
                .uri("/orders/{orderId}/refunds", orderId)
                .header(ChatConsts.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RefundRequest(reason, description))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INVALID_INPUT))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    private record RefundRequest(String reason, String description) {}
}
