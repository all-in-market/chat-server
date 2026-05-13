package com.example.allinmarket.common.config;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    // ── TestController ──────────────────────────────────────────────────────────
    @RestController
    @Validated
    static class TestController {

        @GetMapping("/test/base-exception")
        public void baseException() {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        @PostMapping("/test/validation")
        public void validation(@RequestBody @Valid SampleRequest req) { }

        @PostMapping("/test/not-readable")
        public void notReadable(@RequestBody SampleRequest body) { }

        @GetMapping("/test/constraint")
        public void constraint() {
            throw new ConstraintViolationException("constraint violation", Set.of());
        }

        @GetMapping("/test/type-mismatch")
        public void typeMismatch(@RequestParam SampleEnum value) { }

        @GetMapping("/test/data-conflict")
        public void dataConflict() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }

        @GetMapping("/test/data-integrity")
        public void dataIntegrity() {
            throw new DataIntegrityViolationException("some other integrity error");
        }

        @GetMapping("/test/server-error")
        public void serverError() {
            throw new RuntimeException("unexpected");
        }

        @GetMapping("/test/missing-header")
        public void missingHeader(@RequestHeader("X-Required-Header") String header) { }
    }

    record SampleRequest(@NotBlank String name) { }
    enum SampleEnum { A, B }

    // ── 테스트 케이스 ───────────────────────────────────────────────────────────

    @Test
    void baseException_shouldReturn_errorEnumStatus() throws Exception {
        mockMvc.perform(get("/test/base-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validationException_shouldReturn_400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void httpMessageNotReadable_shouldReturn_400() throws Exception {
        mockMvc.perform(post("/test/not-readable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void constraintViolationException_shouldReturn_400() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void typeMismatch_shouldReturn_400() throws Exception {
        mockMvc.perform(get("/test/type-mismatch").param("value", "INVALID_ENUM_VALUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void dataIntegrity_duplicateKey_shouldReturn_409() throws Exception {
        mockMvc.perform(get("/test/data-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void dataIntegrity_other_shouldReturn_400() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unexpectedException_shouldReturn_500() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingRequestHeader_shouldReturn_400() throws Exception {
        mockMvc.perform(get("/test/missing-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}