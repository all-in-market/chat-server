package com.example.allinmarket.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${api.server.url}")
    private String apiServerUrl;

    @Bean
    public RestClient apiServerRestClient() {
        return RestClient.builder()
                .baseUrl(apiServerUrl)
                .build();
    }
}