package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatHistoryResponse;
import com.example.allinmarket.realtimechat.dto.RealtimeChatRoomResponse;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.facade.RealtimeChatRoomFacade;
import com.example.allinmarket.realtimechat.service.RealtimeChatRoomService;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class RealtimeChatAPIController {
    private final RealtimeChatService chatService;
    private final RealtimeChatRoomService chatRoomService;
    private final RealtimeChatRoomFacade chatRoomFacade;

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<RealtimeChatHistoryResponse>> getChatHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "50") int size,
            Principal principal
    ) {
        if (size < 1 || size > 100) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();

        Long userId = userPrincipal.userId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatService.getChatHistory(roomId, userId, lastMessageId, size)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RealtimeChatRoomResponse>>> getRooms(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();

        Long userId = userPrincipal.userId();

        RealtimeChatSenderType senderType = userPrincipal.senderType();

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatRoomService.getChatRooms(userId, senderType)
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createRoom(
            Principal principal,
            @RequestParam Long buyerId,
            @RequestParam Long sellerId
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();

        Long roomId = chatRoomFacade.createRoom(userPrincipal, buyerId, sellerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        roomId)
        );
    }
}
