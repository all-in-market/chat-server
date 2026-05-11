package com.example.allinmarket.realtimechat.controller;

import com.example.allinmarket.common.security.UserPrincipal;
import com.example.allinmarket.realtimechat.dto.RealtimeChatMessageDto;
import com.example.allinmarket.realtimechat.dto.RealtimeReadDto;
import com.example.allinmarket.realtimechat.enums.RealtimeChatMessageType;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.facade.RealtimeChatFacade;
import com.example.allinmarket.realtimechat.service.RealtimeChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RealtimeChatControllerTest {

    @Mock
    private RealtimeChatService chatService;

    @Mock
    private RealtimeChatFacade chatFacade;

    @InjectMocks
    private RealtimeChatController chatController;

    @Test
    void message_호출시_facade_호출됨() {
        // given
        RealtimeChatMessageDto dto = new RealtimeChatMessageDto(
                RealtimeChatMessageType.TALK,
                10L,
                "이름",
                "안녕하세요",
                Map.of(),
                "temp-123"
        );

        UserPrincipal userPrincipal = new UserPrincipal(1L, RealtimeChatSenderType.BUYER, "token");
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(userPrincipal);

        // when
        chatController.message(dto, authentication);

        // then
        then(chatFacade).should().handleMessage(dto, userPrincipal);
    }

    @Test
    void read_호출시_service_호출됨() {
        // given
        RealtimeReadDto dto = new RealtimeReadDto(10L, 1L, 5L);

        UserPrincipal userPrincipal = new UserPrincipal(1L, RealtimeChatSenderType.BUYER, "token");
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(userPrincipal);

        // when
        chatController.read(dto, authentication);

        // then
        then(chatService).should().read(10L, 1L, 5L);
    }
}


