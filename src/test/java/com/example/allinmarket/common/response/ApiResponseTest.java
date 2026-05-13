package com.example.allinmarket.common.response;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    // ── success ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("success() - success=true, 올바른 status/message/data 설정")
    void success_setsFieldsCorrectly() {
        String data = "result";

        ApiResponse<String> response = ApiResponse.success(SuccessEnum.READ_SUCCESS, data);

        assertThat(response.success()).isTrue();
        assertThat(response.status()).isEqualTo(SuccessEnum.READ_SUCCESS.getStatus());
        assertThat(response.message()).isEqualTo(SuccessEnum.READ_SUCCESS.getMessage());
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("success() - data 가 null 이어도 정상 생성")
    void success_withNullData() {
        ApiResponse<Void> response = ApiResponse.success(SuccessEnum.READ_SUCCESS, null);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
    }

    // ── fail(ErrorEnum) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("fail(ErrorEnum) - success=false, data=null, ErrorEnum 의 status/message 설정")
    void fail_withErrorEnum_setsFieldsCorrectly() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorEnum.INVALID_INPUT);

        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(ErrorEnum.INVALID_INPUT.getStatus());
        assertThat(response.message()).isEqualTo(ErrorEnum.INVALID_INPUT.getMessage());
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    // ── fail(ErrorEnum, String) ────────────────────────────────────────────────

    @Test
    @DisplayName("fail(ErrorEnum, String) - 커스텀 메시지가 ErrorEnum 메시지를 덮어씀")
    void fail_withCustomMessage_overridesEnumMessage() {
        String customMessage = "입력값이 올바르지 않습니다.";

        ApiResponse<Void> response = ApiResponse.fail(ErrorEnum.INVALID_INPUT, customMessage);

        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(ErrorEnum.INVALID_INPUT.getStatus());
        assertThat(response.message()).isEqualTo(customMessage);
        assertThat(response.data()).isNull();
    }
}