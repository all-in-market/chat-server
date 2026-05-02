package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.*;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeChatService {
    private final RealtimeChatMessageRepository chatMessageRepository;
    private final RealtimeReadStatusRepository readStatusRepository;
    private final RealtimeChatParticipantRepository chatParticipantRepository;
    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RedisUnreadService unreadService;
    private final RedisPublisher redisPublisher;

    public RealtimeChatMessage save(RealtimeChatMessageDto dto, Long userId) {
        RealtimeChatMessage chatMessage = RealtimeChatMessage.of(
                dto.roomId(),
                userId,
                dto.message()
        );

        return chatMessageRepository.save(chatMessage);
    }

    @Transactional
    public void read(Long roomId, Long userId, Long messageId) {
        validateParticipant(roomId, userId);

        RealtimeReadStatus readStatus = readStatusRepository.findByRoomIdAndUserId(roomId, userId).orElseGet(
                () -> RealtimeReadStatus.of(roomId, userId, 0L)
        );

        readStatus.updateLastRead(messageId);

        readStatusRepository.save(readStatus);

        unreadService.resetUnread(roomId, userId);

        redisPublisher.publishRead(
                roomId,
                new RealtimeReadDto(roomId, userId, messageId)
        );
    }

    public List<Long> getParticipantIds(Long roomId) {
        return chatParticipantRepository.findUserIdsByRoomId(roomId);
    }

    public void validateParticipant(Long roomId, Long userId) {
        boolean exists = chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(roomId, userId);

        if (!exists) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }

    public RealtimeChatHistoryResponse getChatHistory(Long roomId, Long userId, Long lastMessageId, int size) {
        validateParticipant(roomId, userId);

        List<RealtimeChatMessage> messages = chatMessageRepository.findMessages(
                roomId,
                lastMessageId,
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = messages.size() > size;

        if (hasNext) {
            messages = messages.subList(0, size);
        }

        // 정렬 전 원본 리스트(내림차순)에서 마지막 요소 = 다음 커서
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        List<RealtimeChatMessageResponse> responses = messages.stream()
                .map(RealtimeChatMessageResponse::from)
                .sorted(Comparator.comparing(RealtimeChatMessageResponse::messageId))
                .toList();

        return new RealtimeChatHistoryResponse(responses, nextCursor, hasNext);
    }



    @Transactional
    public void updateLastMessage(Long roomId, String lastMessage, LocalDateTime lastMessageTime) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );

        chatRoomRepository.updateLastMessageIfNewer(roomId, lastMessage, lastMessageTime);
    }
}
