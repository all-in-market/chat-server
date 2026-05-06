package com.example.allinmarket.chat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final SemanticDocumentSplitter semanticDocumentSplitter;

    @PostConstruct
    public void ingestDocuments() {
        try {
            if (isAlreadyIngested()) {
                log.info("이미 인제스천된 문서가 있습니다. 인제스천을 건너뜁니다.");
                return;
            }

            List<Document> documents = List.of(
                    loadDocument("documents/return-policy.txt"),
                    loadDocument("documents/exchange-policy.txt")
            );

            // Semantic Chunking
            List<TextSegment> segments = documents.stream()
                    .flatMap(doc -> semanticDocumentSplitter.split(doc).stream())
                    .toList();

            log.info("[Ingestion] 총 {}개 세그먼트 생성", segments.size());

            // 임베딩 후 저장
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment.text()).content();
                embeddingStore.add(embedding, segment);
            }

            log.info("문서 인제스천 완료");
        } catch (Exception e) {
            log.error("문서 인제스천 실패: {}", e.getMessage());
        }
    }

    private Document loadDocument(String path) {
        try {
            URL resource = getClass().getClassLoader().getResource(path);
            Path filePath = Paths.get(resource.toURI());
            return FileSystemDocumentLoader.loadDocument(filePath);
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.DOCUMENT_LOAD_FAILED);
        }
    }

    private Boolean isAlreadyIngested() {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed("test").content())
                .maxResults(1)
                .build();
        return !embeddingStore.search(request).matches().isEmpty();
    }
}
