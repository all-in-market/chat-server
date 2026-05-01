package com.example.allinmarket.common.config;

import com.example.allinmarket.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/ws-chat/**").permitAll()
                        .pathMatchers("/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/categories").permitAll()
                        .pathMatchers("/seller/auth/**").permitAll()
                        .pathMatchers("/seller/**").hasRole("SELLER")
                        .anyExchange().hasRole("BUYER")
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}