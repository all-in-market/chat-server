package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeChatService {
    private final RealtimeChatMessageRepository chatMessageRepository;
    private final RealtimeReadStatusRepository readStatusRepository;
    private final RealtimeChatParticipantRepository chatParticipantRepository;
    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RedisUnreadService unreadService;

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
    }

    @Transactional
    public List<Long> getParticipantIds(Long roomId) {
        return chatParticipantRepository.findUserIdsByRoomId(roomId);
    }

    @Transactional
    public void enterRoom(Long roomId, Long userId) {
        validateParticipant(roomId, userId);

        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );

        RealtimeChatParticipant chatParticipant = RealtimeChatParticipant.of(
                userId,
                chatRoom
        );

        chatParticipantRepository.save(chatParticipant);
    }

    public void validateParticipant(Long roomId, Long userId) {
        boolean exists = chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(roomId, userId);

        if (!exists) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }
}
