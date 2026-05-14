package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.assistant.PolicyAssistant;
import com.example.allinmarket.chat.assistant.SmallTalkAssistant;
import com.example.allinmarket.chat.client.ApiServerClient;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.dto.ChatRequest;
import com.example.allinmarket.chat.dto.EvaluateResponse;
import com.example.allinmarket.chat.service.ModerationService;
import com.example.allinmarket.common.security.SecurityUtils;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final PolicyAssistant policyAssistant;
    private final SmallTalkAssistant smallTalkAssistant;
    private final IntentClassifier intentClassifier;
    private final ModerationService moderationService;
    private final TaskExecutor chatTaskExecutor;
    private final ContentRetriever contentRetriever;
    private final ApiServerClient apiServerClient;


    @PostMapping(value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {

        SseEmitter emitter = new SseEmitter(ChatConsts.SSE_TIMEOUT);

        chatTaskExecutor.execute(() -> {
            try {

                if (moderationService.isFlagged(request.message())) {
                    emitter.send("부적절한 요청입니다.");
                    emitter.complete();
                    return;
                }

                Long userId = SecurityUtils.getCurrentUserId();

                String intent = intentClassifier.classify(request.message());

                log.info("[Intent] {}", intent);

                TokenStream tokenStream;

                switch (intent) {

                    case "SMALL_TALK" -> {
                        tokenStream = smallTalkAssistant.chat(
                                "smalltalk-" + userId,
                                request.message()
                        );
                    }

                    case "POLICY" -> {
                        tokenStream = policyAssistant.chat(
                                "policy-" + userId,
                                request.message()
                        );
                    }

                    case "TOOL" -> {
                        // 🔥 핵심: Tool 직접 실행 (또는 Tool-aware Assistant로 변경 가능)
                        String result = executeTool(request.message(), token);

                        emitter.send(result);
                        emitter.complete();
                        return;
                    }

                    default -> throw new IllegalStateException("Unknown intent");
                }

                tokenStream
                        .onPartialResponseWithContext((chunk, ctx) -> {
                            try {
                                emitter.send(chunk.text());
                            } catch (Exception e) {
                                ctx.streamingHandle().cancel();
                                emitter.completeWithError(e);
                            }
                        })
                        .onCompleteResponse(r -> emitter.complete())
                        .onError(e -> emitter.completeWithError(e))
                        .start();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping(value = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluateResponse> evaluate(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {

        if (moderationService.isFlagged(request.message())) {
            return ResponseEntity.ok(
                    new EvaluateResponse("[부적절한 내용]", List.of())
            );
        }

        Long userId = SecurityUtils.getCurrentUserId();

        String intent = intentClassifier.classify(request.message());

        log.info("[Intent] message={}, classified={}",
                request.message(),
                intent);

        String memoryId = ChatConsts.SMALL_TALK.equals(intent)
                ? "smalltalk-" + userId
                : "policy-" + userId;

        // SMALL_TALK이면 retrieval 수행 안 함
        List<String> contexts = ChatConsts.SMALL_TALK.equals(intent)
                ? List.of()
                : contentRetriever.retrieve(new Query(request.message()))
                  .stream()
                  .map(content -> content.textSegment().text())
                  .toList();

        StringBuilder sb = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<StreamingHandle> handleRef = new AtomicReference<>();

        TokenStream tokenStream = ChatConsts.SMALL_TALK.equals(intent)
                ? smallTalkAssistant.chat(memoryId, request.message())
                : policyAssistant.chat(memoryId, request.message());

        tokenStream
                .onPartialResponseWithContext((chunk, context) -> {
                    handleRef.set(context.streamingHandle());
                    sb.append(chunk.text());
                })
                .onCompleteResponse(response -> latch.countDown())
                .onError(e -> {
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
                return ResponseEntity.status(504)
                        .body(new EvaluateResponse("[타임아웃]", List.of()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError()
                    .body(new EvaluateResponse("[오류]", List.of()));
        }
        if (errorRef.get() != null) {
            return ResponseEntity.internalServerError()
                    .body(new EvaluateResponse("[오류]", List.of()));
        }
        return ResponseEntity.ok(
                new EvaluateResponse(sb.toString(), contexts)
        );
    }

    private String executeTool(String message, String token) {

        if (message.contains("상품") || message.contains("검색")) {
            return apiServerClient.getProducts(token, extractKeyword(message));
        }

        if (message.contains("주문")) {

            if (message.contains("목록") || message.contains("전체") || message.contains("보여")) {
                return apiServerClient.getOrders(token);
            }

            if (message.contains("조회") || message.contains("상세")) {
                return apiServerClient.getOrder(token, extractOrderId(message));
            }
        }

        if (message.contains("반품") || message.contains("환불")) {
            throw new IllegalStateException("반품은 orderId 필요");
        }

        return "처리 가능한 TOOL 요청이 아닙니다.";
    }

    private String extractKeyword(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        // 불용어 제거
        return message
                .replaceAll("(상품|검색|찾아줘|해줘|알려줘)", "")
                .trim();
    }

    private Long extractOrderId(String message) {
        if (message == null) return null;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return Long.parseLong(matcher.group());
        }

        throw new IllegalArgumentException("orderId를 찾을 수 없습니다.");
    }
}