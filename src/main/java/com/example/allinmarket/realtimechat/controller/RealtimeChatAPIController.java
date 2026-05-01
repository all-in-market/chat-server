package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatHistoryResponse;
import com.example.allinmarket.realtimechat.dto.RealtimeChatRoomResponse;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();

        Long userId = userPrincipal.userId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatService.getChatHistory(roomId, userId, lastMessageId, size)
                )
        );
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RealtimeChatRoomResponse>>> getRooms(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();

        Long userId = userPrincipal.userId();

        RealtimeChatSenderType senderType = userPrincipal.senderType();

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatService.getChatRooms(userId, senderType)
                )
        );
    }
}
