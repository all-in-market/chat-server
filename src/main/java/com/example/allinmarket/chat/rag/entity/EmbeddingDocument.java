package com.example.allinmarket.chat.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "langchain4j_embedding_store")
public class EmbeddingDocument {

    @Id
    @Column(name = "embedding_id")
    private UUID embeddingId;

    @Column(name = "text", columnDefinition = "text")
    private String text;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;
}