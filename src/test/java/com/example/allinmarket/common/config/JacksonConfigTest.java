package com.example.allinmarket.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    @DisplayName("ObjectMapper Bean 등록 테스트")
    void objectMapper_등록() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(JacksonConfig.class);

        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        assertThat(objectMapper).isNotNull();

        assertThat(
                objectMapper.getRegisteredModuleIds()
        ).anyMatch(module ->
                module.toString().contains("jackson-datatype-jsr310")
        );

        context.close();
    }
}