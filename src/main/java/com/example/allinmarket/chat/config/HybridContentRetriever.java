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
        // 1. 벡터 검색
        List<Content> vectorResults = vectorContentRetriever.retrieve(query);
        // 2. 키워드 검색
        List<Content> keywordResults = keywordContentRetriever.retrieve(query);

        log.debug("[Hybrid] 벡터 검색: {}개, 키워드 검색: {}개", vectorResults.size(), keywordResults.size());

        // 3. RRF 점수 계산
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Content> contentMap = new HashMap<>();

        // 벡터 검색 결과 점수 부여
        for (int i = 0; i < vectorResults.size(); i++) {
            Content content = vectorResults.get(i);
            String key = content.textSegment().text();
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            contentMap.put(key, content);
        }

        // 키워드 검색 결과 점수 부여
        for (int i = 0; i < keywordResults.size(); i++) {
            Content content = keywordResults.get(i);
            String key = content.textSegment().text();
            double score = 1.0 / (ChatConsts.RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
            contentMap.put(key, content);
        }

        // 4. RRF 점수 기준 정렬 후 상위 MAX_RESULTS 반환
        List<Content> result = rrfScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(ChatConsts.HYBRID_FINAL_SIZE)
                .map(entry -> contentMap.get(entry.getKey()))
                .toList();

        log.debug("[Hybrid] 최종 결과: {}개", result.size());
        return result;
    }
}