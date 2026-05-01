package com.example.allinmarket.realtimechat.repository;

import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealtimeChatMessageRepository extends JpaRepository<RealtimeChatMessage, Long> {
}
