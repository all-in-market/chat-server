package com.example.allinmarket.chat.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SemanticDocumentSplitterTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private SemanticDocumentSplitter splitter;

    // ────────────────────────────────────────────────────
    // 임베딩 헬퍼
    //
    // 코사인 유사도는 벡터의 방향으로 결정됩니다.
    // - 같은 벡터 → 유사도 1.0 (의미 연속, 같은 청크)
    // - 직교 벡터 → 유사도 0.0 (의미 단절, 새 청크)
    // ────────────────────────────────────────────────────

    private Response<Embedding> similar() {
        return Response.from(Embedding.from(new float[]{1.0f, 0.0f, 0.0f}));
    }

    private Response<Embedding> different() {
        return Response.from(Embedding.from(new float[]{0.0f, 1.0f, 0.0f}));
    }

    // MIN_CHUNK_SIZE = 50
    // BreakIterator가 문장을 분리한 뒤 currentChunk에 누적된 길이가
    // 50자를 넘어야 다음 문장과의 유사도가 낮을 때 청크를 분리합니다.
    // 한글 한 글자는 length() 기준 1이므로 50자 이상 문장을 사용합니다.
    private static final String LONG_SENTENCE =
            "반품 신청은 구매일로부터 7일 이내에 고객센터 또는 앱을 통해 반드시 접수하셔야 하며 그 이후에는 처리가 불가합니다.";
    // length() = 55 → MIN_CHUNK_SIZE(50) 초과 보장

    private static final String SHORT_SENTENCE = "반품 가능합니다.";
    // length() = 8 → MIN_CHUNK_SIZE(50) 미만 보장

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("단일 문장은 청크 1개를 반환한다")
        void split_단일_문장() {
            // 단일 문장은 임베딩 비교 자체가 없으므로 embed stub 불필요
            Document doc = Document.from("반품은 7일 이내 가능합니다.");

            List<TextSegment> result = splitter.split(doc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).isEqualTo("반품은 7일 이내 가능합니다.");
        }
    }

    @Nested
    @DisplayName("청크 분리")
    class ChunkSplitTest {

        @Test
        @DisplayName("유사도가 임계값 이상이면 같은 청크로 합쳐진다")
        void split_유사한_문장은_같은_청크() {
            given(embeddingModel.embed(anyString())).willReturn(similar());

            Document doc = Document.from(
                    "반품은 7일 이내 가능합니다.\n교환도 동일한 기간 내에 신청하세요.");

            List<TextSegment> result = splitter.split(doc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).contains("반품은 7일 이내 가능합니다.");
            assertThat(result.get(0).text()).contains("교환도 동일한 기간 내에 신청하세요.");
        }

        @Test
        @DisplayName("유사도가 임계값 미만이고 청크 크기가 MIN_CHUNK_SIZE 이상이면 새 청크로 분리된다")
        void split_의미가_다른_문장은_다른_청크() {
            given(embeddingModel.embed(anyString()))
                    .willReturn(similar())    // LONG_SENTENCE 임베딩
                    .willReturn(different()); // 두 번째 문장 임베딩 (유사도 0.0 → 분리)

            Document doc = Document.from(LONG_SENTENCE + "\n안녕하세요, 오늘 날씨가 정말 좋네요.");

            List<TextSegment> result = splitter.split(doc);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).text()).contains("반품 신청은");
            assertThat(result.get(1).text()).contains("안녕하세요");
        }

        @Test
        @DisplayName("유사도가 낮아도 현재 청크가 MIN_CHUNK_SIZE 미만이면 분리하지 않는다")
        void split_짧은_청크는_분리하지_않는다() {
            given(embeddingModel.embed(anyString()))
                    .willReturn(similar())    // SHORT_SENTENCE 임베딩
                    .willReturn(different()); // 두 번째 문장 임베딩

            Document doc = Document.from(SHORT_SENTENCE + "\n안녕하세요, 날씨가 좋네요.");

            List<TextSegment> result = splitter.split(doc);

            // currentChunk.length() < 50 → 유사도가 낮아도 분리 안 함
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("여러 문장이 의미 단절 지점에서 올바르게 분리된다")
        void split_여러_문장_의미_단절_분리() {
            String s1 = "반품 신청은 구매일로부터 7일 이내에 고객센터 또는 앱을 통해 반드시 접수하셔야 하며 그 이후에는 처리가 불가합니다.";
            String s2 = "교환 신청도 동일하게 구매일로부터 7일 이내에 가능하며 동일한 절차와 조건이 그대로 적용됩니다.";
            String s3 = "오늘 점심 메뉴는 어떠세요?";

            given(embeddingModel.embed(anyString()))
                    .willReturn(similar())    // s1
                    .willReturn(similar())    // s2 (s1과 유사 → 같은 청크에 추가)
                    .willReturn(different()); // s3 (s2와 다름 → 새 청크)

            Document doc = Document.from(s1 + "\n" + s2 + "\n" + s3);

            List<TextSegment> result = splitter.split(doc);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).text()).contains("반품 신청은");
            assertThat(result.get(0).text()).contains("교환 신청도");
            assertThat(result.get(1).text()).contains("오늘 점심");
        }
    }
}