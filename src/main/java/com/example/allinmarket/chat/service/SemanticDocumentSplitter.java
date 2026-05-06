package com.example.allinmarket.chat.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticDocumentSplitter {

    private final EmbeddingModel embeddingModel;
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final int MIN_CHUNK_SIZE = 50;

    public List<TextSegment> split(Document document) {
        // 1. 문장 단위 분리
        String text = document.text();
        List<String> sentences = splitIntoSentences(text);

        if (sentences.isEmpty()) return List.of();
        if (sentences.size() == 1) return List.of(TextSegment.from(sentences.get(0)));

        // 2. 각 문장 임베딩
        List<Embedding> embeddings = sentences.stream()
                .map(s -> embeddingModel.embed(s).content())
                .toList();

        // 3. 인접 문장 간 유사도 계산 후 청크 분리
        List<TextSegment> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            double similarity = cosineSimilarity(embeddings.get(i - 1), embeddings.get(i));
            log.debug("[SemanticChunk] 문장 {} - {} 유사도: {}", i - 1, i, similarity);

            if (similarity < SIMILARITY_THRESHOLD && currentChunk.length() >= MIN_CHUNK_SIZE) {
                // 의미가 끊기는 지점 → 새 청크 시작
                chunks.add(TextSegment.from(currentChunk.toString().trim()));
                currentChunk = new StringBuilder(sentences.get(i));
            } else {
                // 의미가 연속됨 → 현재 청크에 추가
                currentChunk.append("\n").append(sentences.get(i));
            }
        }

        // 마지막 청크 추가
        if (!currentChunk.isEmpty()) {
            chunks.add(TextSegment.from(currentChunk.toString().trim()));
        }

        log.info("[SemanticChunk] 총 {}개 문장 → {}개 청크", sentences.size(), chunks.size());
        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        return Arrays.stream(text.split("(?<=\\n)"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private double cosineSimilarity(Embedding a, Embedding b) {
        float[] vecA = a.vector();
        float[] vecB = b.vector();

        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < vecA.length; i++) {
            dot += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}