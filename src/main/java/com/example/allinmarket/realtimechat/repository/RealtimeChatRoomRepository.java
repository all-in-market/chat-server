package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RealtimeChatRoomRepository extends JpaRepository<RealtimeChatRoom, Long> {
    @Query("SELECT r FROM RealtimeChatRoom r JOIN RealtimeChatParticipant p " +
            "ON p.realtimeChatRoom.id = r.id WHERE p.userId = :userId " +
            "ORDER BY r.lastMessageTime DESC NULLS LAST ")
    List<RealtimeChatRoom> findRoomsByUserId(@Param("userId") Long userId);

    Optional<RealtimeChatRoom> findByBuyerIdAndSellerId(Long buyerId, Long sellerId);
}
