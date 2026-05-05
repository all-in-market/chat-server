package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeChatRoomFacade {
    private final RealtimeChatRoomService chatRoomService;

    public Long createRoom(UserPrincipal userPrincipal, Long buyerId, Long sellerId) {
        validateCreateRoom(userPrincipal, buyerId, sellerId);

        return chatRoomService.getOrCreateRoom(buyerId, sellerId).getId();
    }

    private void validateCreateRoom(UserPrincipal userPrincipal, Long buyerId, Long sellerId) {
        // 자기 자신과 채팅 방지
        if (buyerId.equals(sellerId)) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        Long userId = userPrincipal.userId();

        RealtimeChatSenderType senderType = userPrincipal.senderType();

        // 본인 검증
        if (!userId.equals(buyerId) && !userId.equals(sellerId)) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }

        // 역할 검증
        if (senderType == RealtimeChatSenderType.BUYER && !userId.equals(buyerId)) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }

        if (senderType == RealtimeChatSenderType.SELLER && !userId.equals(sellerId)) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }
}
