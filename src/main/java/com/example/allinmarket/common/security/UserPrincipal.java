package com.example.allinmarket.common.security;

import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;

public record UserPrincipal(Long userId, RealtimeChatSenderType senderType) {
}
