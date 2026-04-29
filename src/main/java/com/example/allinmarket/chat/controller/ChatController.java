package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.AiAssistant;
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message, @RequestHeader("Authorization") String token) {
        return SecurityUtils.getCurrentUserId().flatMapMany(userId -> assistant.chat(userId, message, token));
    }
}
