package com.example.allinmarket.chat.service;

import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// ────────────────────────────────────────────────────
// LENIENT: null 인수로 moderate()가 호출될 때
// anyString() stub과 인수 불일치가 발생하지만,
// catch 블록으로 빠져 결과가 true가 되는 동작 자체를
// 검증하기 위해 strict 검사를 완화합니다.
// ────────────────────────────────────────────────────
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationModel moderationModel;

    @InjectMocks
    private ModerationService moderationService;

    private void mockModeration(boolean flagged) {
        Moderation moderation = mock(Moderation.class);
        given(moderation.flagged()).willReturn(flagged);
        given(moderationModel.moderate(anyString())).willReturn(Response.from(moderation));
    }

    @Nested
    @DisplayName("isFlagged")
    class IsFlaggedTest {

        @Test
        @DisplayName("정상 메시지는 false를 반환한다")
        void isFlagged_정상_메시지() {
            mockModeration(false);

            boolean result = moderationService.isFlagged("반품 신청하고 싶어요");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("유해 메시지는 true를 반환한다")
        void isFlagged_유해_메시지() {
            mockModeration(true);

            boolean result = moderationService.isFlagged("욕설이 포함된 메시지");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("빈 문자열도 모델에 위임하여 결과를 반환한다")
        void isFlagged_빈_문자열() {
            mockModeration(false);

            boolean result = moderationService.isFlagged("");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 메시지는 내부 예외로 catch 블록에 진입하여 true(차단)를 반환한다")
        void isFlagged_null_메시지() {
            // null → moderate(null) → 내부적으로 예외 발생 → catch → true 반환
            // stub 불일치(anyString vs null)는 LENIENT로 허용,
            // PotentialStubbingProblem 없이 실제 동작(true 반환)을 검증합니다.
            boolean result = moderationService.isFlagged(null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("모델 호출 중 RuntimeException 발생 시 true(차단)를 반환한다")
        void isFlagged_모델_예외_발생() {
            given(moderationModel.moderate(anyString()))
                    .willThrow(new RuntimeException("API 오류"));

            boolean result = moderationService.isFlagged("테스트 메시지");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("모델이 다른 RuntimeException을 던져도 true를 반환한다")
        void isFlagged_일반_예외_발생() {
            given(moderationModel.moderate(anyString()))
                    .willThrow(new IllegalStateException("상태 오류"));

            boolean result = moderationService.isFlagged("테스트 메시지");

            assertThat(result).isTrue();
        }
    }
}