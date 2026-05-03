package com.example.allinmarket.chat.rag.repository;

import com.example.allinmarket.chat.rag.entity.EmbeddingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmbeddingDocumentRepository extends JpaRepository<EmbeddingDocument, UUID> {

    @Query("""
            SELECT e FROM EmbeddingDocument e
            WHERE LOWER(e.text) LIKE CONCAT('%', LOWER(:keyword), '%')
            """)
    List<EmbeddingDocument> findByKeyword(@Param("keyword") String keyword);
}