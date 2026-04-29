package com.example.allinmarket.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    // 임베딩 DB 설정
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${langchain4j.pgvector.host}") String host,
            @Value("${langchain4j.pgvector.port}") int port,
            @Value("${langchain4j.pgvector.database}") String database,
            @Value("${langchain4j.pgvector.user}") String user,
            @Value("${langchain4j.pgvector.password}") String password
    ) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table("langchain4j_embedding_store")
                .dimension(1536)
                .build();
    }

    // 기본 RAG 설정
    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.7)
                .build();
    }
}
