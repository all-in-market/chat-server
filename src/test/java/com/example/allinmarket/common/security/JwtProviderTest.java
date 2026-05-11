package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private JwtProvider jwtProvider;

    // 테스트용 256-bit Base64 인코딩 시크릿 (최소 32 bytes)
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0LTEyMzQ1Njc4OTA=";
    private static final long TEST_EXPIRATION = 3_600_000L; // 1시간

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProvider, "expiration", TEST_EXPIRATION);
        jwtProvider.init(); // @PostConstruct 수동 호출
    }

    // ──────────────────────────────────────────────────────────
    // 토큰 생성 / 검증
    // ──────────────────────────────────────────────────────────

    @Test
    void 토큰_생성_및_검증_성공_테스트() {
        // given
        Long userId = 1L;
        UserRole role = UserRole.BUYER;

        // when
        String token = jwtProvider.generateToken(userId, role);

        // then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void 유효하지_않은_토큰_검증_실패_테스트() {
        // given
        String invalidToken = "this.is.invalid";

        // when & then
        assertThat(jwtProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    void 토큰에서_userId_추출_성공_테스트() {
        // given
        Long userId = 42L;
        String token = jwtProvider.generateToken(userId, UserRole.BUYER);

        // when
        Long extractedId = jwtProvider.getUserId(token);

        // then
        assertThat(extractedId).isEqualTo(userId);
    }

    @Test
    void 토큰에서_role_추출_성공_테스트() {
        // given
        UserRole role = UserRole.SELLER;
        String token = jwtProvider.generateToken(1L, role);

        // when
        UserRole extractedRole = jwtProvider.getRole(token);

        // then
        assertThat(extractedRole).isEqualTo(role);
    }

    // ──────────────────────────────────────────────────────────
    // 만료 시간
    // ──────────────────────────────────────────────────────────

    @Test
    void 토큰_남은_만료_시간_양수_테스트() {
        // given
        String token = jwtProvider.generateToken(1L, UserRole.BUYER);

        // when
        long remaining = jwtProvider.getRemainingExpiration(token);

        // then
        assertThat(remaining).isPositive();
        assertThat(remaining).isLessThanOrEqualTo(TEST_EXPIRATION);
    }

    // ──────────────────────────────────────────────────────────
    // getSenderType – 정상 케이스
    // ──────────────────────────────────────────────────────────

    @Test
    void BUYER_역할_토큰에서_senderType_BUYER_추출_성공_테스트() {
        // given
        String token = jwtProvider.generateToken(1L, UserRole.BUYER);

        // when
        RealtimeChatSenderType senderType = jwtProvider.getSenderType(token);

        // then
        assertThat(senderType).isEqualTo(RealtimeChatSenderType.BUYER);
    }

    @Test
    void SELLER_역할_토큰에서_senderType_SELLER_추출_성공_테스트() {
        // given
        String token = jwtProvider.generateToken(1L, UserRole.SELLER);

        // when
        RealtimeChatSenderType senderType = jwtProvider.getSenderType(token);

        // then
        assertThat(senderType).isEqualTo(RealtimeChatSenderType.SELLER);
    }

    // ──────────────────────────────────────────────────────────
    // getSenderType – 예외 케이스
    // ──────────────────────────────────────────────────────────

    @Test
    void RealtimeChatSenderType에_없는_role이면_SENDER_TYPE_INVALID_예외_발생_테스트() {
        // given
        // getSenderType()의 실행 흐름:
        //   getRole(token).toString() → RealtimeChatSenderType.valueOf() → 실패 시 SENDER_TYPE_INVALID
        //
        // 문제: getRole()은 내부적으로 UserRole.valueOf()를 호출하므로,
        //       토큰 클레임에 UserRole에 없는 문자열을 넣으면 getRole() 단계에서 먼저 터진다.
        //
        // 해결: Spy로 getRole()을 스텁하여 "UserRole에는 없지만 테스트용으로 만든 mock 값"을
        //       반환하도록 한다. mock UserRole의 toString()은 "UNKNOWN_ROLE"을 반환하여
        //       RealtimeChatSenderType.valueOf()가 IllegalArgumentException을 던지도록 유도한다.
        JwtProvider spyProvider = Mockito.spy(jwtProvider);
        String dummyToken = spyProvider.generateToken(1L, UserRole.BUYER);

        UserRole unknownRole = Mockito.mock(UserRole.class);
        Mockito.when(unknownRole.toString()).thenReturn("UNKNOWN_ROLE");
        Mockito.doReturn(unknownRole).when(spyProvider).getRole(dummyToken);

        // when & then
        assertThatThrownBy(() -> spyProvider.getSenderType(dummyToken))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorEnum())
                        .isEqualTo(ErrorEnum.SENDER_TYPE_INVALID));
    }
}