package com.example.allinmarket.common.security;

import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("getName() - userId 를 문자열로 반환")
    void getName_returnsUserIdAsString() {
        UserPrincipal principal = new UserPrincipal(42L, RealtimeChatSenderType.BUYER, "token");

        assertThat(principal.getName()).isEqualTo("42");
    }

    @Test
    @DisplayName("toString() - 토큰 값이 마스킹(****)되어 노출되지 않음")
    void toString_masksToken() {
        UserPrincipal principal = new UserPrincipal(1L, RealtimeChatSenderType.SELLER, "secret.token.value");

        String result = principal.toString();

        assertThat(result).doesNotContain("secret.token.value");
        assertThat(result).contains("****");
    }

    @Test
    @DisplayName("record 필드 - userId, senderType, token 이 올바르게 저장됨")
    void fields_areSetCorrectly() {
        UserPrincipal principal = new UserPrincipal(10L, RealtimeChatSenderType.BUYER, "my.token");

        assertThat(principal.userId()).isEqualTo(10L);
        assertThat(principal.senderType()).isEqualTo(RealtimeChatSenderType.BUYER);
        assertThat(principal.token()).isEqualTo("my.token");
    }
}