package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.AiAssistant;
import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.dto.ChatRequest;
import com.example.allinmarket.chat.service.ModerationService;
import com.example.allinmarket.common.security.SecurityUtils;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiAssistant aiAssistant;
    private final IntentClassifier intentClassifier;
    private final ModerationService moderationService;

    @PostMapping(value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {

        SseEmitter emitter = new SseEmitter(ChatConsts.SSE_TIMEOUT);

        // 유해 콘텐츠 검사
        if (moderationService.isFlagged(request.message())) {
            try {
                emitter.send("[부적절한 내용이 포함되어 있어 답변할 수 없습니다.]");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String intent = intentClassifier.classify(request.message());

        TokenStream tokenStream = ChatConsts.SMALL_TALK.equals(intent)
                ? aiAssistant.smallTalk(userId, request.message())
                : aiAssistant.chat(userId, request.message(), token);

        tokenStream
                .onPartialResponse(chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (Exception e) {
                        log.error("[Chat] 스트리밍 전송 오류", e);
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(response -> emitter.complete())
                .onError(e -> {
                    log.error("[Chat] 스트리밍 오류: {}", e.getMessage());
                    try {
                        emitter.send("[오류가 발생했습니다. 다시 시도해주세요.]");
                    } catch (Exception ex) {
                        log.error("[Chat] 오류 메시지 전송 실패", ex);
                    }
                    emitter.completeWithError(e);
                })
                .start();

        return emitter;
    }
}