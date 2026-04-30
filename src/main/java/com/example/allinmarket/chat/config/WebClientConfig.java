package com.example.allinmarket.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${api.server.url}")
    private String apiServerUrl;

    @Bean
    public WebClient apiServerWebClient() {
        return WebClient.builder()
                .baseUrl(apiServerUrl)
                .build();
    }
}
