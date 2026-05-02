package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface RealtimeChatMessageRepository extends JpaRepository<RealtimeChatMessage, Long> {
    @Query("SELECT m FROM RealtimeChatMessage m WHERE m.roomId = :roomId " +
            "AND (:lastMessageId IS NULL OR m.id < :lastMessageId) ORDER BY m.id DESC")
    List<RealtimeChatMessage> findMessages(@Param("roomId") Long roomId,
                                           @Param("lastMessageId") Long lastMessageId,
                                           Pageable pageable);

    @Query("SELECT COUNT(m) FROM RealtimeChatMessage m WHERE m.roomId = :roomId AND m.id > :lastReadMessageId")
    int countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadMessageId") Long lastReadMessageId);
}
