package com.example.allinmarket.chat.config;

import com.example.allinmarket.chat.consts.ChatConsts;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class KeywordContentRetriever {

    private final JdbcTemplate jdbcTemplate;

    public List<Content> retrieve(Query query) {
        String sql = """
        SELECT embedding_id, text
        FROM langchain4j_embedding_store
        WHERE to_tsvector('simple', text) @@ plainto_tsquery('simple', ?)
        ORDER BY ts_rank(to_tsvector('simple', text), plainto_tsquery('simple', ?)) DESC
        LIMIT ?
        """;
        try {
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        String id = rs.getString("embedding_id");
                        String text = rs.getString("text");
                        TextSegment segment = TextSegment.from(text, Metadata.from("embedding_id", id));
                        return Content.from(segment);
                    },
                    query.text(), query.text(), ChatConsts.HYBRID_CANDIDATE_SIZE
            );
        } catch (Exception e) {
            log.warn("[KeywordSearch] 키워드 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }
}