package com.example.allinmarket.chat.security;

import org.springframework.stereotype.Component;

@Component
public class TokenHolder {

    private final ThreadLocal<String> token = new ThreadLocal<>();

    public void set(String token) {
        this.token.set(token);
    }

    public String get() {
        return this.token.get();
    }

    public void clear() {
        this.token.remove();
    }
}
