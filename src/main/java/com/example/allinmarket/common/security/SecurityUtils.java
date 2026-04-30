package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    private SecurityUtils() {}

    // WebFlux용 - Mono로 반환
    public static Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .switchIfEmpty(Mono.error(new BaseException(ErrorEnum.UNAUTHORIZED)));
    }
}