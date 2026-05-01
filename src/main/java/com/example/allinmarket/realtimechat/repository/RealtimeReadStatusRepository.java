package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RealtimeReadStatusRepository extends JpaRepository<RealtimeReadStatus, Long> {
    Optional<RealtimeReadStatus> findByRoomIdAndUserId(Long roomId, Long userId);
}
