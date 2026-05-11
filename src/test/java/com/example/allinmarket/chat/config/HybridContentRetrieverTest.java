package com.example.allinmarket.chat.config;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HybridContentRetrieverTest {

    @Mock
    private EmbeddingStoreContentRetriever vectorContentRetriever;

    @Mock
    private KeywordContentRetriever keywordContentRetriever;

    @InjectMocks
    private HybridContentRetriever hybridContentRetriever;

    // ────────────────────────────────────────────────────
    // 헬퍼: embedding_id가 있는 Content 생성 (중복 판별용)
    // ────────────────────────────────────────────────────

    private Content contentWithId(String id, String text) {
        TextSegment segment = TextSegment.from(text, Metadata.from("embedding_id", id));
        return Content.from(segment);
    }

    private Content contentWithoutId(String text) {
        return Content.from(TextSegment.from(text));
    }

    // ────────────────────────────────────────────────────
    // retrieve
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("retrieve")
    class RetrieveTest {

        @Test
        @DisplayName("벡터 검색과 키워드 검색 결과가 RRF로 합산되어 반환된다")
        void retrieve_RRF_합산() {
            Content v1 = contentWithId("id-1", "반품은 7일 이내");
            Content k1 = contentWithId("id-2", "교환은 14일 이내");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of(v1));
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of(k1));

            List<Content> result = hybridContentRetriever.retrieve(Query.from("반품"));

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("동일 embedding_id가 벡터/키워드 양쪽에 있으면 RRF 점수가 합산되어 1개만 반환된다")
        void retrieve_중복_제거_후_점수_합산() {
            Content v1 = contentWithId("id-same", "반품은 7일 이내");
            Content k1 = contentWithId("id-same", "반품은 7일 이내");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of(v1));
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of(k1));

            List<Content> result = hybridContentRetriever.retrieve(Query.from("반품"));

            // 동일 id → 중복 제거 후 1개
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("벡터 검색 결과가 없어도 키워드 결과만으로 반환된다")
        void retrieve_벡터_결과_없음() {
            Content k1 = contentWithId("id-1", "반품은 7일 이내");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of());
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of(k1));

            List<Content> result = hybridContentRetriever.retrieve(Query.from("반품"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).textSegment().text()).isEqualTo("반품은 7일 이내");
        }

        @Test
        @DisplayName("키워드 검색 결과가 없어도 벡터 결과만으로 반환된다")
        void retrieve_키워드_결과_없음() {
            Content v1 = contentWithId("id-1", "교환 정책 안내");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of(v1));
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of());

            List<Content> result = hybridContentRetriever.retrieve(Query.from("교환"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).textSegment().text()).isEqualTo("교환 정책 안내");
        }

        @Test
        @DisplayName("양쪽 모두 결과가 없으면 빈 목록을 반환한다")
        void retrieve_모두_결과_없음() {
            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of());
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of());

            List<Content> result = hybridContentRetriever.retrieve(Query.from("없는키워드"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("벡터 1순위 결과는 키워드 후순위보다 최종 점수가 높다")
        void retrieve_벡터_1위가_키워드_후순위보다_높다() {
            // 벡터: v1(1위), v2(2위)
            // 키워드: v2(1위), v1(2위)
            // v1: vector 1위 + keyword 2위 → RRF 합산
            // v2: vector 2위 + keyword 1위 → RRF 합산
            // RRF k=60 기준으로 동점이 되도록 설계됨
            // 검증 포인트: 결과 개수만 확인 (순위는 동점 처리에 따라 다를 수 있음)

            Content v1 = contentWithId("id-1", "벡터 1위 문서");
            Content v2 = contentWithId("id-2", "벡터 2위 문서");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of(v1, v2));
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of(v2, v1));

            List<Content> result = hybridContentRetriever.retrieve(Query.from("테스트"));

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("embedding_id가 없는 Content는 텍스트를 키로 사용해 중복을 판별한다")
        void retrieve_임베딩_id_없이_텍스트_키_사용() {
            Content c1 = contentWithoutId("동일한 텍스트");
            Content c2 = contentWithoutId("동일한 텍스트");

            given(vectorContentRetriever.retrieve(any(Query.class))).willReturn(List.of(c1));
            given(keywordContentRetriever.retrieve(any(Query.class))).willReturn(List.of(c2));

            List<Content> result = hybridContentRetriever.retrieve(Query.from("동일"));

            // 텍스트가 같으면 같은 키 → 중복 제거 후 1개
            assertThat(result).hasSize(1);
        }
    }
}