package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RealtimeChatParticipantRepository extends JpaRepository<RealtimeChatParticipant, Long> {
    boolean existsByRealtimeChatRoomIdAndUserId(Long roomId, Long userId);

    @Query("SELECT p.userId FROM RealtimeChatParticipant p WHERE p.realtimeChatRoom.id = :roomId")
    List<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);
}
