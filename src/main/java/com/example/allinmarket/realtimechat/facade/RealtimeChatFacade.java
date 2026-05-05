package com.example.allinmarket.realtimechat.facade;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.enums.RealtimeChatMessageType;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.service.RealtimeChatProvider;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import com.example.allinmarket.realtimechat.service.RedisUnreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeChatFacade {
    private final RealtimeChatService chatService;
    private final RedisUnreadService unreadService;
    private final RedisPublisher redisPublisher;
    private final RealtimeChatProvider chatProvider;
    private final SimpMessageSendingOperations messageSendingOperations;

    @Transactional
    public void sendMessage(RealtimeChatMessageDto dto, Long senderId, RealtimeChatSenderType senderType) {
        // 참여자 검증
        chatService.validateParticipant(dto.roomId(), senderId);

        // 메세지 저장
        RealtimeChatMessage saved = chatService.save(dto, senderId);

        // 마지막 메세지 저장
        chatService.updateLastMessage(saved.getRoomId(), saved.getMessage(), saved.getCreatedAt());

        // 참여자 조회
        List<Long> participants = chatService.getParticipantIds(dto.roomId());

        String senderName = chatProvider.getUserName(senderId, senderType);

        Long roomId = saved.getRoomId();
        String message = saved.getMessage();
        String tempId = dto.tempId();

        afterCommit(() -> {
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
                    RealtimeChatMessageType.TALK,
                    roomId,
                    senderName,
                    message,
                    unreadMap,
                    tempId
            );

            redisPublisher.publish(dto.roomId(), response);
        });
    }

    @Transactional
    public void enterRoom(RealtimeChatMessageDto dto, Long userId, RealtimeChatSenderType senderType) {
        // 참여자 검증
        chatService.validateParticipant(dto.roomId(), userId);

        String senderName = chatProvider.getUserName(userId, senderType);

        // 입장 이벤트 생성 (메세지 X)
        RealtimeChatMessageDto messageDto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.ENTER,
                dto.roomId(),
                senderName,
                null, // 메세지는 없음
                Map.of(), // unread 없음
                dto.tempId()
        );

        afterCommit(() -> {
            // unread 초기화 (입장 시 읽음 처리)
            unreadService.resetUnread(dto.roomId(), userId);

            redisPublisher.publish(dto.roomId(), messageDto);
        });
    }

    @Transactional
    public void handleMessage(RealtimeChatMessageDto dto, UserPrincipal userPrincipal) {
        try {
            chatService.validateParticipant(dto.roomId(), userPrincipal.userId());

            if (dto.type() == null){
                throw new BaseException(ErrorEnum.MESSAGE_TYPE_INVALID);
            }

            switch (dto.type()) {
                case ENTER -> enterRoom(dto, userPrincipal.userId(), userPrincipal.senderType());
                case TALK -> sendMessage(dto, userPrincipal.userId(), userPrincipal.senderType());
                default -> throw new BaseException(ErrorEnum.MESSAGE_TYPE_INVALID);
            }

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendAck(userPrincipal.userId(), dto.tempId(), "SUCCESS");
                        }
                    }
            );

        } catch (BaseException e) {
            log.error("채팅 처리 중 비즈니스 예외 발생: {}", e.getMessage());
            sendAck(userPrincipal.userId(), dto.tempId(), "ERROR_" + e.getErrorEnum());
            throw e;

        } catch (Exception e) {
            log.error("시스템 장애로 인한 채팅 전송 실패", e);
            sendAck(userPrincipal.userId(), dto.tempId(), "SYSTEM_ERROR");
            throw e;
        }
    }

    private void sendAck(Long userId, String tempId, String status) {
        if (tempId == null) return;

        messageSendingOperations.convertAndSendToUser(
                userId.toString(),
                "/sub/chat/status",
                Map.of("tempId", tempId, "status", status)
        );
    }

    private void afterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            task.run();
                        } catch (Exception e) {
                            log.error("afterCommit 실패", e);
                        }
                    }
                }
        );
    }
}
