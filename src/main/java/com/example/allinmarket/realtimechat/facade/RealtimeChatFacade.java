package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import com.example.allinmarket.realtimechat.service.RedisUnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeChatFacade {
    private final RealtimeChatService chatService;
    private final RedisUnreadService unreadService;
    private final RedisPublisher redisPublisher;

    @Transactional
    public void sendMessage(RealtimeChatMessageDto dto, Long senderId) {
        // 참여자 검증
        chatService.validateParticipant(dto.roomId(), senderId);

        // 메세지 저장
        RealtimeChatMessage saved = chatService.save(dto, senderId);

        // 참여자 조회
        List<Long> participants = chatService.getParticipantIds(dto.roomId());

        // unread 증가
        for (Long userId : participants) {
            if (!userId.equals(senderId)) {
                unreadService.incrementUnread(dto.roomId(), userId);
            }
        }

        // unread 맵 생성
        Map<Long, Integer> unreadMap = new HashMap<>();

        for (Long userId : participants) {
            unreadMap.put(userId, unreadService.getUnread(dto.roomId(), userId));
        }

        RealtimeChatMessageDto response = new RealtimeChatMessageDto(
                dto.type(),
                saved.getRoomId(),
                dto.senderName(),
                saved.getMessage(),
                unreadMap
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisPublisher.publish(dto.roomId(), response);
                    }
                }
        );
    }
}
