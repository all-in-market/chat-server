package com.example.allinmarket.chat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    @DisplayName("RestClient 생성 성공")
    void apiServerRestClient_생성() {

        RestClientConfig config =
                new RestClientConfig();

        ReflectionTestUtils.setField(
                config,
                "apiServerUrl",
                "http://localhost:8080"
        );

        RestClient client =
                config.apiServerRestClient();

        assertThat(client).isNotNull();
    }
}