package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.realtimechat.dto.RealtimeChatHistoryResponse;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class RealtimeChatAPIController {
    private final RealtimeChatService chatService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<RealtimeChatHistoryResponse>> getChatHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "50") int size,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatService.getChatHistory(roomId, userId, lastMessageId, size)
                )
        );
    }
}
