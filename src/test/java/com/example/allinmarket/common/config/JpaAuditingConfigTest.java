package com.example.allinmarket.common.config;

import com.example.allinmarket.common.entity.DeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class JpaAuditingTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("createdAt이 자동 저장된다")
    void created_at_created() {

        TestEntity entity = new TestEntity();

        em.persist(entity);
        em.flush();
        em.clear();

        TestEntity saved =
                em.find(TestEntity.class, entity.getId());

        assertThat(saved.getCreatedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("updatedAt이 자동 수정된다")
    void updated_at_updated() throws InterruptedException {

        TestEntity entity = new TestEntity();

        em.persist(entity);
        em.flush();
        em.clear();

        TestEntity saved =
                em.find(TestEntity.class, entity.getId());

        LocalDateTime before =
                saved.getUpdatedAt();

        Thread.sleep(100);

        saved.changeName("수정");

        em.flush();
        em.clear();

        TestEntity updated =
                em.find(TestEntity.class, entity.getId());

        assertThat(updated.getUpdatedAt())
                .isAfter(before);
    }

    @Test
    @DisplayName("delete 호출 시 deletedAt 저장")
    void deleted_at_saved() {

        TestEntity entity = new TestEntity();

        em.persist(entity);

        entity.delete();

        em.flush();
        em.clear();

        TestEntity deleted =
                em.find(TestEntity.class, entity.getId());

        assertThat(deleted.getDeletedAt())
                .isNotNull();
    }

    @Entity
    @EntityListeners(AuditingEntityListener.class)
    static class TestEntity extends DeletableEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void changeName(String name) {
            this.name = name;
        }
    }
}