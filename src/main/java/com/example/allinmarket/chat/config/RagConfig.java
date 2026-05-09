package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.SQLException;

@Configuration
public class RagConfig {

    // 임베딩 DB 설정
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            DataSource dataSource,
            @Value("${langchain4j.pgvector.dimension}") int dimension
    ) throws SQLException {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            throw new IllegalStateException(
                    "Expected HikariDataSource but got: " + dataSource.getClass().getName()
            );
        }

        String jdbcUrl = hikari.getJdbcUrl();
        URI uri = URI.create(jdbcUrl.replace("jdbc:", ""));

        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("Invalid JDBC URL (database name missing): " + jdbcUrl);
        }

        int port = (uri.getPort() == -1) ? 5432 : uri.getPort();

        return PgVectorEmbeddingStore.builder()
                .host(uri.getHost())
                .port(port)
                .database(path.substring(1))
                .user(hikari.getUsername())
                .password(hikari.getPassword())
                .table("langchain4j_embedding_store")
                .dimension(dimension)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate) {

        EmbeddingStoreContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(ChatConsts.HYBRID_CANDIDATE_SIZE)
                .minScore(0.5)
                .build();

        KeywordContentRetriever keywordRetriever = new KeywordContentRetriever(jdbcTemplate);

        return new HybridContentRetriever(vectorRetriever, keywordRetriever);
    }
}
