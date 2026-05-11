package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    // 각 테스트 후 SecurityContext 초기화
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────

    private void setAuthentication(UserPrincipal userPrincipal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ──────────────────────────────────────────────────────────
    // getUserPrincipal – 정상
    // ──────────────────────────────────────────────────────────

    @Test
    void 인증된_사용자_UserPrincipal_반환_성공_테스트() {
        // given
        UserPrincipal expected = new UserPrincipal(1L, RealtimeChatSenderType.BUYER, "valid-token");
        setAuthentication(expected);

        // when
        UserPrincipal result = SecurityUtils.getUserPrincipal();

        // then
        assertThat(result).isEqualTo(expected);
    }

    // ──────────────────────────────────────────────────────────
    // getUserPrincipal – 예외: authentication == null
    // ──────────────────────────────────────────────────────────

    @Test
    void authentication이_null이면_UNAUTHORIZED_예외_발생_테스트() {
        // given — SecurityContext에 아무것도 세팅하지 않음

        // when & then
        assertThatThrownBy(SecurityUtils::getUserPrincipal)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    // ──────────────────────────────────────────────────────────
    // getUserPrincipal – 예외: AnonymousAuthenticationToken
    // ──────────────────────────────────────────────────────────

    @Test
    void AnonymousAuthentication이면_UNAUTHORIZED_예외_발생_테스트() {
        // given
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        // when & then
        assertThatThrownBy(SecurityUtils::getUserPrincipal)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    // ──────────────────────────────────────────────────────────
    // getUserPrincipal – 예외: principal이 UserPrincipal 타입 아님
    // ──────────────────────────────────────────────────────────

    @Test
    void principal이_UserPrincipal_타입이_아니면_UNAUTHORIZED_예외_발생_테스트() {
        // given — principal을 String으로 세팅
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("not-a-user-principal", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when & then
        assertThatThrownBy(SecurityUtils::getUserPrincipal)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    // ──────────────────────────────────────────────────────────
    // getCurrentUserId
    // ──────────────────────────────────────────────────────────

    @Test
    void getCurrentUserId_정상_반환_테스트() {
        // given
        setAuthentication(new UserPrincipal(42L, RealtimeChatSenderType.SELLER, "token"));

        // when & then
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(42L);
    }

    // ──────────────────────────────────────────────────────────
    // getCurrentSenderType
    // ──────────────────────────────────────────────────────────

    @Test
    void getCurrentSenderType_정상_반환_테스트() {
        // given
        setAuthentication(new UserPrincipal(1L, RealtimeChatSenderType.SELLER, "token"));

        // when & then
        assertThat(SecurityUtils.getCurrentSenderType()).isEqualTo(RealtimeChatSenderType.SELLER);
    }

    // ──────────────────────────────────────────────────────────
    // getCurrentToken – 정상
    // ──────────────────────────────────────────────────────────

    @Test
    void getCurrentToken_정상_반환시_Bearer_접두사_포함_테스트() {
        // given
        setAuthentication(new UserPrincipal(1L, RealtimeChatSenderType.BUYER, "abc123"));

        // when
        String token = SecurityUtils.getCurrentToken();

        // then
        assertThat(token).isEqualTo("Bearer abc123");
    }

    // ──────────────────────────────────────────────────────────
    // getCurrentToken – 예외: 토큰 없음
    // ──────────────────────────────────────────────────────────

    @Test
    void getCurrentToken_토큰이_빈값이면_UNAUTHORIZED_예외_발생_테스트() {
        // given
        setAuthentication(new UserPrincipal(1L, RealtimeChatSenderType.BUYER, ""));

        // when & then
        assertThatThrownBy(SecurityUtils::getCurrentToken)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }

    @Test
    void getCurrentToken_토큰이_null이면_UNAUTHORIZED_예외_발생_테스트() {
        // given
        setAuthentication(new UserPrincipal(1L, RealtimeChatSenderType.BUYER, null));

        // when & then
        assertThatThrownBy(SecurityUtils::getCurrentToken)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.UNAUTHORIZED));
    }
}