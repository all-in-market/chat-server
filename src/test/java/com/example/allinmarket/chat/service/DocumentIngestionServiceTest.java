package com.example.allinmarket.chat.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private SemanticDocumentSplitter semanticDocumentSplitter;

    @InjectMocks
    private DocumentIngestionService documentIngestionService;

    // ────────────────────────────────────────────────────
    // 헬퍼: isAlreadyIngested() 결과 제어
    // embeddingModel.embed("test") → queryEmbedding으로 사용
    // embeddingStore.search() → matches() 반환값으로 인제스천 여부 결정
    // ────────────────────────────────────────────────────

    private void mockAlreadyIngested(boolean ingested) {
        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f, 0.2f});
        given(embeddingModel.embed("test"))
                .willReturn(Response.from(dummyEmbedding));

        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        given(searchResult.matches())
                .willReturn(ingested ? List.of(mock(dev.langchain4j.store.embedding.EmbeddingMatch.class)) : List.of());
        given(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .willReturn(searchResult);
    }

    // ────────────────────────────────────────────────────
    // ingestDocuments
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ingestDocuments")
    class IngestDocumentsTest {

        @Test
        @DisplayName("이미 인제스천된 경우 임베딩 저장을 수행하지 않는다")
        void ingestDocuments_이미_인제스천됨() {
            mockAlreadyIngested(true);

            documentIngestionService.ingestDocuments();

            // 임베딩 저장(add)이 호출되지 않아야 함
            verify(embeddingStore, never()).add(any(Embedding.class), any(TextSegment.class));
            // 문서 분리도 호출되지 않아야 함
            verify(semanticDocumentSplitter, never()).split(any());
        }

        @Test
        @DisplayName("인제스천 안 된 경우 세그먼트를 임베딩하여 저장한다")
        void ingestDocuments_정상_인제스천() {
            mockAlreadyIngested(false);

            // 문서 분리 결과 stub
            TextSegment seg1 = TextSegment.from("반품은 7일 이내 가능합니다.");
            TextSegment seg2 = TextSegment.from("교환은 14일 이내 가능합니다.");
            given(semanticDocumentSplitter.split(any()))
                    .willReturn(List.of(seg1))  // return-policy.txt
                    .willReturn(List.of(seg2)); // exchange-policy.txt

            // 각 세그먼트 임베딩 stub
            Embedding emb1 = Embedding.from(new float[]{0.1f, 0.2f});
            Embedding emb2 = Embedding.from(new float[]{0.3f, 0.4f});
            given(embeddingModel.embed(seg1.text())).willReturn(Response.from(emb1));
            given(embeddingModel.embed(seg2.text())).willReturn(Response.from(emb2));

            documentIngestionService.ingestDocuments();

            // 세그먼트 수만큼 add 호출 확인
            verify(embeddingStore, times(2)).add(any(Embedding.class), any(TextSegment.class));
        }

        @Test
        @DisplayName("인제스천 시 올바른 세그먼트가 저장된다")
        void ingestDocuments_올바른_세그먼트_저장() {
            mockAlreadyIngested(false);

            TextSegment seg = TextSegment.from("반품은 7일 이내 가능합니다.");
            given(semanticDocumentSplitter.split(any()))
                    .willReturn(List.of(seg))
                    .willReturn(List.of());

            Embedding emb = Embedding.from(new float[]{0.5f});
            given(embeddingModel.embed(seg.text())).willReturn(Response.from(emb));

            documentIngestionService.ingestDocuments();

            ArgumentCaptor<TextSegment> segCaptor = ArgumentCaptor.forClass(TextSegment.class);
            ArgumentCaptor<Embedding> embCaptor = ArgumentCaptor.forClass(Embedding.class);
            verify(embeddingStore).add(embCaptor.capture(), segCaptor.capture());

            assertThat(segCaptor.getValue().text()).isEqualTo("반품은 7일 이내 가능합니다.");
            assertThat(embCaptor.getValue()).isEqualTo(emb);
        }

        @Test
        @DisplayName("isAlreadyIngested 중 예외 발생 시 예외를 전파하지 않고 로그만 남긴다")
        void ingestDocuments_검사_중_예외_발생() {
            given(embeddingModel.embed("test"))
                    .willThrow(new RuntimeException("임베딩 모델 오류"));

            // 예외가 외부로 전파되지 않아야 함
            org.assertj.core.api.Assertions.assertThatCode(
                    () -> documentIngestionService.ingestDocuments()
            ).doesNotThrowAnyException();
        }
    }
}