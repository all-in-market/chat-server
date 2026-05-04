package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${api.server.url}")
    private String apiServerUrl;

    @Bean
    public RestClient apiServerRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ChatConsts.CONNECT_TIMEOUT);
        factory.setReadTimeout(ChatConsts.READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(apiServerUrl)
                .build();
    }
}