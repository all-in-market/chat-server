package com.example.allinmarket.chat.config;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordContentRetrieverTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private KeywordContentRetriever keywordContentRetriever;

    @Nested
    @DisplayName("retrieve")
    class RetrieveTest {

        @Test
        @DisplayName("정상 쿼리 시 DB 조회 결과를 Content 목록으로 반환한다")
        void retrieve_정상_조회() {
            Content mockContent = Content.from(
                    dev.langchain4j.data.segment.TextSegment.from("반품은 7일 이내 가능합니다."));
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willReturn(List.of(mockContent));

            List<Content> result = keywordContentRetriever.retrieve(Query.from("반품 정책"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).textSegment().text()).isEqualTo("반품은 7일 이내 가능합니다.");
        }

        // Query.from()은 langchain4j 내부에서 blank 문자열에 대해 예외를 던지므로
        // 빈/공백 입력 테스트는 Query를 mock으로 직접 생성합니다.

        @Test
        @DisplayName("공백만 있는 쿼리 텍스트는 빈 목록을 반환하고 DB를 호출하지 않는다")
        void retrieve_공백_쿼리() {
            Query query = mock(Query.class);
            given(query.text()).willReturn("   ");

            List<Content> result = keywordContentRetriever.retrieve(query);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("빈 문자열 쿼리 텍스트는 빈 목록을 반환하고 DB를 호출하지 않는다")
        void retrieve_빈_문자열_쿼리() {
            Query query = mock(Query.class);
            given(query.text()).willReturn("");

            List<Content> result = keywordContentRetriever.retrieve(query);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("1글자 토큰만 있는 쿼리는 fallback 키워드로 DB를 조회한다")
        void retrieve_단글자_토큰() {
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willReturn(List.of());

            // "a" → length 1 → 2글자 미만 → tokens[0]="a" (fallback) → not blank → DB 조회
            List<Content> result = keywordContentRetriever.retrieve(Query.from("a"));

            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
        }

        @Test
        @DisplayName("여러 토큰 중 2글자 이상인 첫 토큰을 키워드로 사용한다")
        void retrieve_여러_토큰_키워드_추출() {
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willReturn(List.of());

            keywordContentRetriever.retrieve(Query.from("반품 교환 정책"));

            ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(anyString(), any(RowMapper.class), argsCaptor.capture());
            assertThat((String) argsCaptor.getValue()[0]).contains("반품");
        }

        @Test
        @DisplayName("DB 조회 중 예외 발생 시 빈 목록을 반환한다")
        void retrieve_DB_예외() {
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willThrow(new RuntimeException("DB 오류"));

            List<Content> result = keywordContentRetriever.retrieve(Query.from("반품 정책"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("DB 조회 결과가 없으면 빈 목록을 반환한다")
        void retrieve_결과_없음() {
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willReturn(List.of());

            List<Content> result = keywordContentRetriever.retrieve(Query.from("존재하지않는키워드"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("검색 패턴에 % 와일드카드가 포함된다")
        void retrieve_LIKE_패턴_검증() {
            given(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .willReturn(List.of());

            keywordContentRetriever.retrieve(Query.from("반품"));

            ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(anyString(), any(RowMapper.class), argsCaptor.capture());
            assertThat((String) argsCaptor.getValue()[0]).startsWith("%").endsWith("%").contains("반품");
        }
    }
}