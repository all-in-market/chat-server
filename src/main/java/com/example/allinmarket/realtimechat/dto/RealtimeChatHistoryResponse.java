package com.example.allinmarket.realtimechat.dto;

import java.util.List;

public record RealtimeChatHistoryResponse(
        List<RealtimeChatMessageResponse> responses,
        Long nextCursor,
        boolean hasNext
) {
}
