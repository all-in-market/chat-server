package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HybridContentRetriever implements ContentRetriever{

    private final EmbeddingStoreContentRetriever vectorContentRetriever;
    private final KeywordContentRetriever keywordContentRetriever;

    public List<Content> retrieve(Query query) {
        log.info("[Hybrid] 검색 시작: 벡터+키워드");

        // 1. 벡터 검색
        List<Content> vectorResults = vectorContentRetriever.retrieve(query);
        log.debug("[Vector] {}개 결과", vectorResults.size());

        // 2. 키워드 검색
        List<Content> keywordResults = keywordContentRetriever.retrieve(query);
        log.debug("[Keyword] {}개 결과", keywordResults.size());

        // 3. RRF 점수 계산
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Content> contentMap = new HashMap<>();

        // 벡터 검색 결과 점수 부여
        for (int i = 0; i < vectorResults.size(); i++) {
            Content content = vectorResults.get(i);
            String key = getKey(content);
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            contentMap.put(key, content);
            log.debug("[RRF-Keyword] 순위: {}, 점수: {}", i + 1, String.format("%.4f", score));
        }

        // 키워드 검색 결과 점수 부여
        for (int i = 0; i < keywordResults.size(); i++) {
            Content content = keywordResults.get(i);
            String key = getKey(content);
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            contentMap.put(key, content);
            log.debug("[RRF-Keyword] 순위: {}, 점수: {}", i + 1, String.format("%.4f", score));
        }

        // 4. RRF 점수 기준 정렬 후 상위 MAX_RESULTS 반환
        List<Content> result = rrfScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .peek(entry -> log.debug("[RRF-Final] 최종 점수: {}", String.format("%.4f", entry.getValue())))
                .limit(ChatConsts.HYBRID_FINAL_SIZE)
                .map(entry -> contentMap.get(entry.getKey()))
                .toList();

        log.info("[Hybrid] 최종: {}개 반환", result.size());
        return result;
    }

    private String getKey(Content content) {
        String embeddingId = content.textSegment().metadata().getString("embedding_id");
        return embeddingId != null ? embeddingId : content.textSegment().text();
    }
}