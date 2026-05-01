package com.example.allinmarket.chat.service;

import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.moderation.ModerationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationModel moderationModel;

    public Mono<Boolean> isFlagged(String message) {
        return Mono.fromCallable(() -> {
                    Moderation result = moderationModel.moderate(message).content();
                    return result.flagged();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(flagged -> {
                    if (flagged) {
                        log.warn("[Moderation] 유해 콘텐츠 감지 (length={}): [REDACTED]",
                                message == null ? 0 : message.length());
                    }
                });
    }
}
