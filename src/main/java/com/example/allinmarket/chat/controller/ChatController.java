package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.AiAssistant;
import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.dto.ChatRequest;
import com.example.allinmarket.chat.service.ModerationService;
import com.example.allinmarket.common.security.SecurityUtils;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

        AtomicReference<StreamingHandle> handleRef = new AtomicReference<>();

        tokenStream
                .onPartialResponseWithContext((chunk, context) -> {
                    handleRef.set(context.streamingHandle());
                    try {
                        emitter.send(chunk.text());
                    } catch (Exception e) {
                        log.error("[Chat] 스트리밍 전송 오류", e);
                        context.streamingHandle().cancel();
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

        emitter.onTimeout(() -> {
            log.info("[Chat] SSE 타임아웃");
            emitter.complete();
        });

        emitter.onCompletion(() ->
                log.info("[Chat] SSE 연결 종료")
        );

        emitter.onError(e -> {
            log.error("[Chat] SSE 오류", e);
            StreamingHandle handle = handleRef.get();
            if (handle != null) handle.cancel();
        });

        return emitter;
    }

    @PostMapping(value = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> evaluate(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {

        if (moderationService.isFlagged(request.message())) {
            return ResponseEntity.ok("[부적절한 내용이 포함되어 있어 답변할 수 없습니다.]");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String intent = intentClassifier.classify(request.message());

        StringBuilder sb = new StringBuilder();
        AtomicReference<StreamingHandle> handleRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        TokenStream tokenStream = ChatConsts.SMALL_TALK.equals(intent)
                ? aiAssistant.smallTalk(userId, request.message())
                : aiAssistant.chat(userId, request.message(), token);

        CountDownLatch latch = new CountDownLatch(1);

        tokenStream
                .onPartialResponseWithContext((chunk, context) -> {
                    handleRef.set(context.streamingHandle());
                    sb.append(chunk.text());
                })
                .onCompleteResponse(response -> latch.countDown())
                .onError(e -> {
                    log.error("[Evaluate] 오류: {}", e.getMessage());
                    errorRef.set(e);
                    latch.countDown();
                })
                .start();

        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                StreamingHandle handle = handleRef.get();
                if (handle != null) {
                    handle.cancel();
                }
                return ResponseEntity.status(504).body("[응답 생성 시간이 초과되었습니다. 다시 시도해주세요.]");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            StreamingHandle handle = handleRef.get();
            if (handle != null) {
                handle.cancel();
            }
            return ResponseEntity.internalServerError().body("[요청 처리 중 오류가 발생했습니다.]");
        }
        if (errorRef.get() != null) {
            return ResponseEntity.internalServerError().body("[오류가 발생했습니다. 다시 시도해주세요.]");
        }
        return ResponseEntity.ok(sb.toString());
    }
}