package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealtimeChatParticipantRepository extends JpaRepository<RealtimeChatParticipant, Long> {
    boolean existsByRealtimeChatRoomIdAndUserId(Long roomId, Long userId);
}
