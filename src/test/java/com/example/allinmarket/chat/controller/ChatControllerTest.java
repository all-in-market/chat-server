package com.example.allinmarket.chat.controller;

import com.example.allinmarket.chat.assistant.IntentClassifier;
import com.example.allinmarket.chat.assistant.PolicyAssistant;
import com.example.allinmarket.chat.assistant.SmallTalkAssistant;
import com.example.allinmarket.chat.consts.ChatConsts;
import com.example.allinmarket.chat.dto.ChatRequest;
import com.example.allinmarket.chat.service.ModerationService;
import com.example.allinmarket.common.config.GlobalExceptionHandler;
import com.example.allinmarket.common.config.SecurityConfig;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.support.RestDocsControllerTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        ChatController.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class
})
class ChatControllerTest extends RestDocsControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PolicyAssistant policyAssistant;

    @MockitoBean
    private SmallTalkAssistant smallTalkAssistant;

    @MockitoBean
    private IntentClassifier intentClassifier;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private TaskExecutor chatTaskExecutor;

    @MockitoBean
    private ContentRetriever contentRetriever;

    @BeforeEach
    void setUpWithSecurity(RestDocumentationContextProvider restDocumentation)
            throws Exception {

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .apply(springSecurity())
                .build();

        doAnswer(invocation -> {

            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;

        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class),
                any(ServletResponse.class),
                any(FilterChain.class)
        );
    }

    private void setAuthContext(Long userId) {

        UserPrincipal userPrincipal =
                new UserPrincipal(
                        userId,
                        RealtimeChatSenderType.BUYER,
                        "test-token"
                );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private TokenStream mockTokenStream(String fullText) {

        TokenStream ts = mock(TokenStream.class);

        given(ts.onPartialResponseWithContext(any()))
                .willReturn(ts);

        given(ts.onCompleteResponse(any()))
                .willAnswer(invocation -> {

                    Consumer<ChatResponse> cb =
                            invocation.getArgument(0);

                    cb.accept(ChatResponse.builder()
                            .aiMessage(AiMessage.from(fullText))
                            .build());

                    return ts;
                });

        given(ts.onError(any()))
                .willReturn(ts);

        return ts;
    }

    // ─────────────────────────────────────────────
    // POST /chat/stream
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /chat/stream")
    class StreamTest {

        @Test
        @DisplayName("정상 요청 시 200과 text/event-stream 반환")
        void stream_정상_요청() throws Exception {

            setAuthContext(1L);

            mockMvc.perform(post("/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token")
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("반품 정책이 어떻게 돼요?")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.TEXT_EVENT_STREAM
                    ));
        }

        @Test
        @DisplayName("Authorization 헤더 없으면 400")
        void stream_Authorization_헤더_없음() throws Exception {

            setAuthContext(1L);

            mockMvc.perform(post("/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("안녕")
                            )))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("요청 바디 없으면 400")
        void stream_요청_바디_없음() throws Exception {

            setAuthContext(1L);

            mockMvc.perform(post("/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // POST /chat/evaluate
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /chat/evaluate")
    class EvaluateTest {

        @Test
        @DisplayName("INQUIRY 의도 - RAG 검색 후 답변 생성 성공")
        void evaluate_INQUIRY_성공() throws Exception {

            setAuthContext(1L);

            given(moderationService.isFlagged(anyString()))
                    .willReturn(false);

            given(intentClassifier.classify(anyString()))
                    .willReturn(ChatConsts.INQUIRY);

            Content mockContent = mock(Content.class);

            dev.langchain4j.data.segment.TextSegment segment =
                    dev.langchain4j.data.segment.TextSegment.from(
                            "반품은 7일 이내 가능합니다."
                    );

            given(mockContent.textSegment())
                    .willReturn(segment);

            given(contentRetriever.retrieve(any(Query.class)))
                    .willReturn(List.of(mockContent));

            TokenStream ts =
                    mockTokenStream(
                            "반품은 구매일로부터 7일 이내에 신청하실 수 있습니다."
                    );

            given(policyAssistant.chat(anyString(), anyString()))
                    .willReturn(ts);

            mockMvc.perform(post("/chat/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token")
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("반품 기간이 얼마나 돼요?")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").exists())
                    .andExpect(jsonPath("$.contexts").isArray())
                    .andDo(document("chat/evaluate/inquiry",
                            requestFields(
                                    fieldWithPath("message")
                                            .description("사용자 메시지")
                            ),
                            responseFields(
                                    fieldWithPath("answer")
                                            .description("AI 답변"),
                                    fieldWithPath("contexts")
                                            .description("RAG 검색 결과")
                            )
                    ));

            verify(contentRetriever)
                    .retrieve(any(Query.class));
        }

        @Test
        @DisplayName("SMALL_TALK 의도 - 검색 없이 일상 대화")
        void evaluate_SMALL_TALK_성공() throws Exception {

            setAuthContext(1L);

            given(moderationService.isFlagged(anyString()))
                    .willReturn(false);

            given(intentClassifier.classify(anyString()))
                    .willReturn(ChatConsts.SMALL_TALK);

            TokenStream ts =
                    mockTokenStream(
                            "안녕하세요! 무엇을 도와드릴까요?"
                    );

            given(smallTalkAssistant.chat(anyString(), anyString()))
                    .willReturn(ts);

            mockMvc.perform(post("/chat/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token")
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("안녕하세요")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").exists())
                    .andExpect(jsonPath("$.contexts").isArray())
                    .andExpect(jsonPath("$.contexts.length()").value(0))
                    .andDo(document("chat/evaluate/small-talk",
                            requestFields(
                                    fieldWithPath("message")
                                            .description("사용자 메시지")
                            ),
                            responseFields(
                                    fieldWithPath("answer")
                                            .description("일상 대화 응답"),
                                    fieldWithPath("contexts")
                                            .description("빈 배열")
                            )
                    ));

            // 핵심 검증:
            // SMALL_TALK에서는 retrieval 절대 수행되지 않아야 함
            verify(contentRetriever, never())
                    .retrieve(any(Query.class));
        }

        @Test
        @DisplayName("유해 콘텐츠 감지 시 부적절한 내용 반환")
        void evaluate_유해_콘텐츠_감지() throws Exception {

            setAuthContext(1L);

            given(moderationService.isFlagged(anyString()))
                    .willReturn(true);

            mockMvc.perform(post("/chat/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token")
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("욕설이 포함된 메시지")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer")
                            .value("[부적절한 내용]"))
                    .andExpect(jsonPath("$.contexts").isArray())
                    .andExpect(jsonPath("$.contexts.length()").value(0))
                    .andDo(document("chat/evaluate/flagged",
                            requestFields(
                                    fieldWithPath("message")
                                            .description("사용자 메시지")
                            ),
                            responseFields(
                                    fieldWithPath("answer")
                                            .description("차단 응답"),
                                    fieldWithPath("contexts")
                                            .description("빈 배열")
                            )
                    ));

            verify(moderationService)
                    .isFlagged(anyString());
        }

        @Test
        @DisplayName("Authorization 헤더 없으면 400")
        void evaluate_Authorization_헤더_없음() throws Exception {

            setAuthContext(1L);

            mockMvc.perform(post("/chat/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("반품 신청하고 싶어요")
                            )))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("요청 바디 없으면 400")
        void evaluate_요청_바디_없음() throws Exception {

            setAuthContext(1L);

            mockMvc.perform(post("/chat/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isBadRequest());
        }
    }
}
