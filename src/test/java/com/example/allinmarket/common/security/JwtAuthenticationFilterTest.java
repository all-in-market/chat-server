package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN   = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";

    @Mock
    JwtProvider jwtProvider;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PingController())
                .addFilter(new JwtAuthenticationFilter(jwtProvider))  // 실제 필터 주입
                .build();
    }

    // ── PingController ────────────────────────────────────────────────────────
    @RestController
    static class PingController {
        @GetMapping("/ping")
        public ResponseEntity<String> ping() {
            return ResponseEntity.ok("pong");
        }
    }

    // ── 테스트 케이스 ─────────────────────────────────────────────────────────

    @Test
    void validToken_shouldReturn_200() throws Exception {
        when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtProvider.getUserId(VALID_TOKEN)).thenReturn(1L);
        when(jwtProvider.getRole(VALID_TOKEN)).thenReturn(UserRole.BUYER);

        mockMvc.perform(get("/ping")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void invalidToken_shouldReturn_401_fromFilter() throws Exception {
        when(jwtProvider.validateToken(INVALID_TOKEN)).thenReturn(false);

        mockMvc.perform(get("/ping")
                        .header("Authorization", "Bearer " + INVALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void noToken_shouldReturn_200_withoutAuthentication() throws Exception {
        /**
         * standaloneSetup 은 Spring Security 필터 체인을 포함하지 않으므로
         * 토큰 없음 → SecurityContext 미설정 → 인가 필터 없음 → 컨트롤러 그대로 실행 → 200.
         * "토큰 없을 때 403" 은 SecurityConfig(hasRole) 의 동작이지
         * JwtAuthenticationFilter 자체의 동작이 아니므로 이 테스트에서는 검증 범위 밖이다.
         */
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }
}