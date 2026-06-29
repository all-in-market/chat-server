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

        // 기존 PostgreSQL db 안에 flyway로 생성해둔 "langchain4j_embedding_store" 라는 이름의 PGVector 전용 테이블을 사용할 것이라고 연결하는 설정
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

    // vector DB 에서 문서를 검색할 검색기를 생성하는 코드
    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate) {

        // 의미적으로 유사한 결과만 조회하는 검색기
        EmbeddingStoreContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(ChatConsts.HYBRID_CANDIDATE_SIZE)
                .minScore(0.5)
                .build();

        // 해당 keyword를 포함하는 결과를 조회하는 검색기
        KeywordContentRetriever keywordRetriever = new KeywordContentRetriever(jdbcTemplate);

        // 두 검색기를 합친 하이브리드 검색기를 사용
        return new HybridContentRetriever(vectorRetriever, keywordRetriever);
    }
}
