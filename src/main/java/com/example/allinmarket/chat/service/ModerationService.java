package com.example.allinmarket.chat.service;

import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.moderation.ModerationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 유해성 검사용
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationModel moderationModel;

    public boolean isFlagged(String message) {
        try {
            Moderation result = moderationModel.moderate(message).content();
            boolean flagged = result.flagged();
            if (flagged) {
                log.warn("[Moderation] 유해 콘텐츠 감지 (length={}): [REDACTED]",
                        message == null ? 0 : message.length());
            }
            return flagged;
        } catch (Exception e) {
            log.error("[Moderation] 검사 실패", e);
            return true;
        }
    }
}
