package com.example.allinmarket.chat.consts;

import java.time.Duration;

public final class ChatConsts {

    // Http
    public static final String AUTHORIZATION = "Authorization";

    // Intent
    public static final String SMALL_TALK = "SMALL_TALK";
    public static final String INQUIRY = "INQUIRY";

    // Chat Memory
    public static final String MEMORY_KEY_PREFIX = "chat:memory:";
    public static final Duration MEMORY_TTL = Duration.ofHours(24);

    // Token Store
    public static final String TOKEN_KEY_PREFIX = "chat:token:";
    public static final Duration TOKEN_TTL = Duration.ofHours(1);
}
