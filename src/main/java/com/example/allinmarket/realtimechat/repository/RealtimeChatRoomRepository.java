package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RealtimeChatRoomRepository extends JpaRepository<RealtimeChatRoom, Long> {
    @Query("SELECT r FROM RealtimeChatRoom r JOIN RealtimeChatParticipant p " +
            "ON p.realtimeChatRoom.id = r.id WHERE p.userId = :userId " +
            "ORDER BY r.lastMessageTime DESC NULLS LAST ")
    List<RealtimeChatRoom> findRoomsByUserId(@Param("userId") Long userId);

    Optional<RealtimeChatRoom> findByBuyerIdAndSellerId(Long buyerId, Long sellerId);

    @Modifying
    @Query("UPDATE RealtimeChatRoom r SET r.lastMessage = :lastMessage, r.lastMessageTime = :lastMessageTime " +
            "WHERE r.id = :roomId AND (r.lastMessageTime IS NULL OR r.lastMessageTime < :lastMessageTime)")
    int updateLastMessageIfNewer(@Param("roomId") Long roomId,
                                 @Param("lastMessage") String lastMessage,
                                 @Param("lastMessageTime")LocalDateTime lastMessageTime);
}
