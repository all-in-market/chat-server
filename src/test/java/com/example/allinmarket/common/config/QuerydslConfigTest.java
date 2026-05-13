package com.example.allinmarket.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QuerydslConfigTest {

    @Test
    @DisplayName("JPAQueryFactory 생성 성공")
    void jpaQueryFactory_생성() {

        QuerydslConfig config =
                new QuerydslConfig();

        EntityManager em =
                mock(EntityManager.class);

        ReflectionTestUtils.setField(
                config,
                "em",
                em
        );

        JPAQueryFactory queryFactory =
                config.jpaQueryFactory();

        assertThat(queryFactory).isNotNull();
    }
}