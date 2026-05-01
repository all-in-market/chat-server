package com.example.allinmarket.chat.security;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TokenHolder {

    private static final String TOKEN_KEY = "token";

    public Mono<String> get() {
        return Mono.deferContextual(ctx ->
                Mono.just(ctx.get(TOKEN_KEY))
        );
    }
}
