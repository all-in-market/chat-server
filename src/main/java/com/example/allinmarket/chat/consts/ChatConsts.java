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

    // Hybrid Search
    public static final int TOP_K = 20;       // 각 검색에서 가져올 후보 수
    public static final int FINAL_TOP_K = 3;  // 최종 반환할 문서 수
    public static final int RRF_K = 60;        // RRF 상수 (논문 권장값)
    public static final double MIN_SCORE = 0.5; // 최소 유사도 점수
}
