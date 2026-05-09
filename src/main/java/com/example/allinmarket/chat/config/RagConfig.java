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
        HikariDataSource hikari = (HikariDataSource) dataSource;
        String jdbcUrl = hikari.getJdbcUrl();

        URI uri = URI.create(jdbcUrl.replace("jdbc:", ""));

        return PgVectorEmbeddingStore.builder()
                .host(uri.getHost())
                .port(uri.getPort())
                .database(uri.getPath().substring(1))
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
