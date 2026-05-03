package com.example.allinmarket.chat.rag;

import com.example.allinmarket.chat.consts.ChatConsts;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HybridContentRetriever implements ContentRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();

        // 1. Dense 검색 (벡터 유사도)
        List<EmbeddingMatch<TextSegment>> denseResults = denseSearch(queryText);

        // 2. Sparse 검색 (키워드 LIKE)
        List<EmbeddingMatch<TextSegment>> sparseResults = sparseSearch(queryText, denseResults);

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

    private List<EmbeddingMatch<TextSegment>> sparseSearch(
            String queryText,
            List<EmbeddingMatch<TextSegment>> denseResults) {
        // Dense 결과에서 키워드 매칭으로 필터링 (LIKE 방식)
        String lowerQuery = queryText.toLowerCase();
        return denseResults.stream()
                .filter(match -> {
                    String content = match.embedded().text().toLowerCase();
                    // 쿼리 단어 중 하나라도 포함되면 매칭
                    String[] words = lowerQuery.split("\\s+");
                    for (String word : words) {
                        if (content.contains(word)) return true;
                    }
                    return false;
                })
                .toList();
    }

    private List<TextSegment> reciprocalRankFusion(
            List<EmbeddingMatch<TextSegment>> denseResults,
            List<EmbeddingMatch<TextSegment>> sparseResults) {

        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, TextSegment> segmentMap = new HashMap<>();

        // Dense 결과에 RRF 점수 부여
        for (int i = 0; i < denseResults.size(); i++) {
            EmbeddingMatch<TextSegment> match = denseResults.get(i);
            String key = match.embedded().text();
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            segmentMap.put(key, match.embedded());
        }

        // Sparse 결과에 RRF 점수 부여
        for (int i = 0; i < sparseResults.size(); i++) {
            EmbeddingMatch<TextSegment> match = sparseResults.get(i);
            String key = match.embedded().text();
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            segmentMap.putIfAbsent(key, match.embedded());
        }

        // RRF 점수 기준 정렬 후 상위 FINAL_TOP_K 반환
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(ChatConsts.FINAL_TOP_K)
                .map(entry -> segmentMap.get(entry.getKey()))
                .toList();
    }
}