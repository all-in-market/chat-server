package com.example.allinmarket.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RagConfigTest {

    private final RagConfig ragConfig =
            new RagConfig();

    @Nested
    @DisplayName("contentRetriever")
    class ContentRetrieverTest {

        @Test
        @DisplayName("HybridContentRetriever 생성 성공")
        void contentRetriever_생성() {

            EmbeddingStore<TextSegment> embeddingStore =
                    mock(EmbeddingStore.class);

            EmbeddingModel embeddingModel =
                    mock(EmbeddingModel.class);

            JdbcTemplate jdbcTemplate =
                    mock(JdbcTemplate.class);

            ContentRetriever retriever =
                    ragConfig.contentRetriever(
                            embeddingStore,
                            embeddingModel,
                            jdbcTemplate
                    );

            assertThat(retriever)
                    .isNotNull()
                    .isInstanceOf(HybridContentRetriever.class);
        }
    }
}