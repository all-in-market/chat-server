package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.AiAssistant;
import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final AiAssistant assistant;
    private final IntentClassifier intentClassifier;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message, @RequestHeader(ChatConsts.AUTHORIZATION) String token) {

        return SecurityUtils.getCurrentUserId().flatMapMany(userId -> {
            String intent = intentClassifier.classify(message);

            if(ChatConsts.SMALL_TALK.equals(intent)) {
                return assistant.smallTalk(userId, message);
            }
                return assistant.chat(userId, message, token);
        });
    }
}
