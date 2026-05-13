package com.example.allinmarket.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    @Test
    @DisplayName("PasswordEncoder Bean 등록 테스트")
    void passwordEncoder_등록() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(PasswordEncoderConfig.class);

        PasswordEncoder passwordEncoder =
                context.getBean(PasswordEncoder.class);

        String encoded = passwordEncoder.encode("1234");

        assertThat(passwordEncoder.matches("1234", encoded))
                .isTrue();

        context.close();
    }
}