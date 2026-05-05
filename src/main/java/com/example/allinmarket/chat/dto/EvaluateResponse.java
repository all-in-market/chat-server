package com.example.allinmarket.chat.dto;

import java.util.List;

public record EvaluateResponse(
        String answer,
        List<String> contexts
) {}