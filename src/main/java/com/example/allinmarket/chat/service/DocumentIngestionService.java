package com.example.allinmarket.chat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
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

    @PostConstruct
    public void ingestDocuments() {
        try {
            // 이미 데이터가 있으면 스킵
            if(isAlreadyIngested()) {
                log.info("이미 인제스천된 문서가 있습니다. 인제스천을 건너뜁니다.");
                return;
            }

            // 문서 로드
            List<Document> documents = List.of(
                loadDocument("documents/return-policy.txt"),
                loadDocument("documents/exchange-policy.txt")
            );

            // 청킹 설정
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

            // 인제스천 파이프라인
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

            ingestor.ingest(documents);
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
