package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.AiAssistant;
import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.dto.ChatRequest;
import com.example.allinmarket.chat.security.TokenHolder;
import com.example.allinmarket.chat.service.ModerationService;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final AiAssistant aiAssistant;
    private final IntentClassifier intentClassifier;
    private final ModerationService moderationService;
    private final TokenHolder tokenHolder;

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> stream(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {
        return moderationService.isFlagged(request.message())
                .flatMapMany(flagged -> {
                    if (flagged) {
                        return Flux.just("[부적절한 내용이 포함되어 있어 답변할 수 없습니다.]");
                    }
                    tokenHolder.set(token);
                    return SecurityUtils.getCurrentUserId()
                            .flatMapMany(userId ->
                                    Mono.fromCallable(() -> intentClassifier.classify(request.message()))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .flatMapMany(intent -> {
                                                if ("SMALL_TALK".equals(intent)) {
                                                    return aiAssistant.smallTalk(userId, request.message());
                                                }
                                                return aiAssistant.chat(userId, request.message());
                                            })
                            );
                })
                .doFinally(signal -> tokenHolder.clear())
                .doOnError(e -> log.error("[Chat] 스트리밍 오류: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just("[오류가 발생했습니다. 다시 시도해주세요.]"))
                .doOnCancel(() -> log.info("[Chat] 클라이언트 연결 끊김"));
    }
}