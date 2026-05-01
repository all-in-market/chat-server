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

@Service
@RequiredArgsConstructor
public class RealtimeChatService {
    private final RealtimeChatMessageRepository realtimeChatMessageRepository;
    private final RealtimeReadStatusRepository realtimeReadStatusRepository;
    private final RealtimeChatParticipantRepository realtimeChatParticipantRepository;
    private final RealtimeChatRoomRepository realtimeChatRoomRepository;

    public RealtimeChatMessage save(RealtimeChatMessageDto dto, Long userId) {
        RealtimeChatMessage realtimeChatMessage = RealtimeChatMessage.of(
                dto.roomId(),
                userId,
                dto.message()
        );

        return realtimeChatMessageRepository.save(realtimeChatMessage);
    }

    @Transactional
    public void read(Long roomId, Long userId, Long messageId) {
        RealtimeReadStatus readStatus = realtimeReadStatusRepository.findByRoomIdAndUserId(roomId, userId).orElseGet(
                () -> RealtimeReadStatus.of(roomId, userId, 0L)
        );

        readStatus.updateLastRead(messageId);

        realtimeReadStatusRepository.save(readStatus);
    }

    public void validateParticipant(Long roomId, Long userId) {
        boolean exists = realtimeChatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(roomId, userId);

        if (!exists) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }

    @Transactional
    public void enterRoom(Long roomId, Long userId) {
        validateParticipant(roomId, userId);

        RealtimeChatRoom realtimeChatRoom = realtimeChatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );

        RealtimeChatParticipant realtimeChatParticipant = RealtimeChatParticipant.of(
                userId,
                realtimeChatRoom
        );

        realtimeChatParticipantRepository.save(realtimeChatParticipant);
    }
}
