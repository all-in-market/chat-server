package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatHistoryResponse;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageResponse;
import com.example.allinmarket.realtimechat.dto.RealtimeChatRoomResponse;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.facade.RealtimeChatRoomFacade;
import com.example.allinmarket.realtimechat.service.RealtimeChatRoomService;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RealtimeChatAPIController.class)
class RealtimeChatAPIControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private RealtimeChatService chatService;

    @MockitoBean
    private RealtimeChatRoomService chatRoomService;

    @MockitoBean
    private RealtimeChatRoomFacade chatRoomFacade;

    private Principal mockPrincipal() {
        // UserPrincipal(userId, senderType, token)
        UserPrincipal userPrincipal = new UserPrincipal(1L, RealtimeChatSenderType.BUYER, "dummy-token");
        return new TestingAuthenticationToken(userPrincipal, null);
    }

    @Test
    @WithMockUser
    void 채팅_히스토리_조회_성공_테스트() throws Exception {
        RealtimeChatMessageResponse msg = new RealtimeChatMessageResponse(1L, 1L, 10L,"안녕하세요", LocalDateTime.now());
        RealtimeChatHistoryResponse response = new RealtimeChatHistoryResponse(List.of(msg), 1L, false);

        given(chatService.getChatHistory(eq(10L), eq(1L), any(), eq(50)))
                .willReturn(response);

        mockMvc.perform(get("/chat/rooms/{roomId}/messages", 10L)
                        .principal(mockPrincipal())
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.responses[0].message").value("안녕하세요"))
                .andDo(document("chat/history",
                        pathParameters(
                                parameterWithName("roomId").description("채팅방 ID")
                        ),
                        queryParameters(
                                parameterWithName("lastMessageId").optional().description("마지막 메시지 ID (페이징용)"),
                                parameterWithName("size").optional().description("조회할 메시지 개수 (기본값 50, 최대 100)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.responses[].messageId").description("메시지 ID"),
                                fieldWithPath("data.responses[].roomId").description("채팅방 ID"),
                                fieldWithPath("data.responses[].senderId").description("보낸 사람 ID"),
                                fieldWithPath("data.responses[].message").description("메시지 내용"),
                                fieldWithPath("data.responses[].createdAt").description("보낸 시각"),
                                fieldWithPath("data.nextCursor").description("다음 커서 ID"),
                                fieldWithPath("data.hasNext").description("다음 페이지 존재 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 채팅방_목록조회_성공_테스트() throws Exception {
        RealtimeChatRoomResponse roomResponse = new RealtimeChatRoomResponse(10L, "테스트방", "마지막 메시지", null, 0, "상대방");
        given(chatRoomService.getChatRooms(eq(1L), eq(RealtimeChatSenderType.BUYER)))
                .willReturn(List.of(roomResponse));

        mockMvc.perform(get("/chat/rooms").principal(mockPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data[0].roomId").value(10))
                .andExpect(jsonPath("$.data[0].roomName").value("테스트방"))
                .andDo(document("chat/rooms",
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].roomId").description("채팅방 ID"),
                                fieldWithPath("data[].roomName").description("채팅방 이름"),
                                fieldWithPath("data[].lastMessage").description("마지막 메시지"),
                                fieldWithPath("data[].lastMessageTime").description("마지막 메시지 시각"),
                                fieldWithPath("data[].unreadCount").description("읽지 않은 메시지 수"),
                                fieldWithPath("data[].opponentName").description("상대방 이름"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 채팅방_생성_성공_테스트() throws Exception {
        given(chatRoomFacade.createRoom(any(UserPrincipal.class), eq(2L), eq(3L)))
                .willReturn(99L);

        mockMvc.perform(post("/chat/rooms?buyerId=2&sellerId=3")
                        .principal(mockPrincipal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").value(99))
                .andDo(document("chat/create-room",
                        queryParameters(
                                parameterWithName("buyerId").description("구매자 ID"),
                                parameterWithName("sellerId").description("판매자 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("생성된 채팅방 ID"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }
}
