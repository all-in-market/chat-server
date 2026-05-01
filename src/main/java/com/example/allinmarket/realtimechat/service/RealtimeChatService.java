package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RealtimeChatService {
    private final RealtimeChatMessageRepository realtimeChatMessageRepository;
    private final RealtimeReadStatusRepository realtimeReadStatusRepository;

    public RealtimeChatMessage save(RealtimeChatMessageDto dto) {
        RealtimeChatMessage realtimeChatMessage = RealtimeChatMessage.of(
                dto.roomId(),
                dto.senderId(),
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
}
