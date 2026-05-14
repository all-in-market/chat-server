package com.example.allinmarket.chat.consts;

import java.time.Duration;

public final class ChatConsts {

    // Http
    public static final String AUTHORIZATION = "Authorization";

    // Intent
    public static final String SMALL_TALK = "SMALL_TALK";
    public static final String INQUIRY = "INQUIRY";
    public static final String TOOL = "TOOL";

    // Chat Memory
    public static final String MEMORY_KEY_PREFIX = "chat:memory:";
    public static final Duration MEMORY_TTL = Duration.ofDays(30);

    // SSE
    public static final long SSE_TIMEOUT = 180_000L; // 3분

    // RestClient Timeout
    public static final int CONNECT_TIMEOUT = 5000;  // 5초
    public static final int READ_TIMEOUT = 10000;    // 10초

    // Hybrid Search
    public static final int HYBRID_CANDIDATE_SIZE = 10;  // RRF 후보군 크기
    public static final int HYBRID_FINAL_SIZE = 3;        // RRF 최종 결과 크기
    public static final int RRF_K = 60;                   // RRF 상수
}
