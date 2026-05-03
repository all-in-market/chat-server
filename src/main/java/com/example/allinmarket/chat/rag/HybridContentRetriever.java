package com.example.allinmarket.chat.rag;

import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.rag.entity.EmbeddingDocument;
import com.example.allinmarket.chat.rag.repository.EmbeddingDocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HybridContentRetriever implements ContentRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingDocumentRepository embeddingDocumentRepository;

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();

        if (!StringUtils.hasText(queryText)) {
            return List.of();
        }

        // 1. Dense 검색 (벡터 유사도)
        List<EmbeddingMatch<TextSegment>> denseResults = denseSearch(queryText);

        // 2. Sparse 검색 (DB 직접 키워드 검색 - Dense와 독립적)
        List<EmbeddingDocument> sparseResults = sparseSearch(queryText);

        // 3. RRF로 점수 결합
        List<TextSegment> reranked = reciprocalRankFusion(denseResults, sparseResults);

        // 4. Content로 변환
        return reranked.stream()
                .map(Content::from)
                .toList();
    }

    private List<EmbeddingMatch<TextSegment>> denseSearch(String queryText) {
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(ChatConsts.TOP_K)
                .minScore(ChatConsts.MIN_SCORE)
                .build();
        return embeddingStore.search(request).matches();
    }

    private List<EmbeddingDocument> sparseSearch(String queryText) {
        if(!StringUtils.hasText(queryText)) {
            return List.of();
        }
        String[] words = queryText.trim().toLowerCase().split("\\s+");

        return java.util.Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .flatMap(word -> embeddingDocumentRepository.findByKeyword(word).stream())
                .collect(java.util.stream.Collectors.toMap(
                        EmbeddingDocument::getEmbeddingId,
                        doc -> doc,
                        (first, second) -> first, java.util.LinkedHashMap::new))
                .values()
                .stream()
                .limit(ChatConsts.TOP_K)
                .toList();
    }

    private List<TextSegment> reciprocalRankFusion(
            List<EmbeddingMatch<TextSegment>> denseResults,
            List<EmbeddingDocument> sparseResults) {

        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, TextSegment> segmentMap = new HashMap<>();

        // Dense 결과에 RRF 점수 부여 (embeddingId를 키로 사용)
        for (int i = 0; i < denseResults.size(); i++) {
            EmbeddingMatch<TextSegment> match = denseResults.get(i);
            UUID key = parseUuidOrNull(match.embeddingId());
            if (key == null) {
                log.warn("[HybridSearch] 유효하지 않은 embeddingId로 Dense 결과 건너뜀: {}", match.embeddingId());
                continue;
            }
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            segmentMap.put(key, match.embedded());
        }

        // Sparse 결과에 RRF 점수 부여
        for (int i = 0; i < sparseResults.size(); i++) {
            EmbeddingDocument doc = sparseResults.get(i);
            UUID key = doc.getEmbeddingId();
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            segmentMap.putIfAbsent(key, TextSegment.from(doc.getText()));
        }

        // RRF 점수 기준 정렬 후 상위 FINAL_TOP_K 반환
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(ChatConsts.FINAL_TOP_K)
                .map(entry -> segmentMap.get(entry.getKey()))
                .toList();
    }

    private UUID parseUuidOrNull(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}