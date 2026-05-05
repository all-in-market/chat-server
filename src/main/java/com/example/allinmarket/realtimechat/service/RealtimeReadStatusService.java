package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeReadStatusService {
    private final RealtimeReadStatusRepository readStatusRepository;
    private final RealtimeChatMessageRepository chatMessageRepository;

    public int calculateUnreadFromDB(Long roomId, Long userId) {
        Long lastReadId = readStatusRepository.findByRoomIdAndUserId(roomId, userId)
                .map(RealtimeReadStatus::getLastReadMessageId)
                .orElse(0L);

        return chatMessageRepository.countUnreadMessages(roomId, lastReadId);
    }
}
