package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

// 키워드 기반 검색기
@Slf4j
@RequiredArgsConstructor
public class KeywordContentRetriever {

    private final JdbcTemplate jdbcTemplate;

    public List<Content> retrieve(Query query) {
        String[] tokens = query.text().split("\\s+");
        if (tokens.length == 0 || (tokens.length == 1 && tokens[0].isEmpty())) {
            return List.of();
        }
        String keyword = Arrays.stream(tokens)
                .filter(t -> t.length() >= 2)
                .findFirst()
                .orElse(tokens[0]);

        if (keyword.isBlank()) {
            return List.of();
        }

        String sql = """
        SELECT embedding_id, text
        FROM langchain4j_embedding_store
        WHERE text ILIKE ?
        LIMIT ?
        """;
        try {
            String searchPattern = "%" + keyword + "%";
            List<Content> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        String id = rs.getString("embedding_id");
                        String text = rs.getString("text");
                        TextSegment segment = TextSegment.from(text, Metadata.from("embedding_id", id));
                        return Content.from(segment);
                    },
                    searchPattern, ChatConsts.HYBRID_CANDIDATE_SIZE
            );
            return results;
        } catch (Exception e) {
            log.warn("[KeywordSearch] 키워드 검색 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }
}