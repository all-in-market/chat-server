package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.realtimechat.dto.RealtimeChatRoomResponse;
import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RealtimeChatRoomServiceTest {

    @Mock
    private RealtimeChatRoomRepository chatRoomRepository;

    @Mock
    private RealtimeChatParticipantRepository chatParticipantRepository;

    @Mock
    private RedisUnreadService unreadService;

    @Mock
    private RealtimeChatProvider chatProvider;

    @InjectMocks
    private RealtimeChatRoomService chatRoomService;

    @Test
    void 기존_채팅방_조회_성공_테스트() {
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);
        given(chatRoomRepository.findByBuyerIdAndSellerId(1L, 2L))
                .willReturn(Optional.of(room));

        RealtimeChatRoom result = chatRoomService.getOrCreateRoom(1L, 2L);

        assertThat(result).isEqualTo(room);
        then(chatRoomRepository).should().findByBuyerIdAndSellerId(1L, 2L);
    }

    @Test
    void 신규_채팅방_생성_성공_테스트() {
        given(chatRoomRepository.findByBuyerIdAndSellerId(1L, 2L))
                .willReturn(Optional.empty());

        RealtimeChatRoom savedRoom = RealtimeChatRoom.of(1L, 2L);
        given(chatRoomRepository.save(any())).willReturn(savedRoom);

        RealtimeChatRoom result = chatRoomService.getOrCreateRoom(1L, 2L);

        assertThat(result).isEqualTo(savedRoom);
        then(chatParticipantRepository).should().save(argThat(p -> p.getUserId().equals(1L)));
        then(chatParticipantRepository).should().save(argThat(p -> p.getUserId().equals(2L)));
    }

    @Test
    void 채팅방_목록_조회_성공_테스트() {
        // given
        RealtimeChatRoom room = RealtimeChatRoom.of(1L, 2L);
        ReflectionTestUtils.setField(room, "id", 10L);

        given(chatRoomRepository.findRoomsByUserId(1L))
                .willReturn(List.of(room));

        RealtimeChatParticipant buyer = RealtimeChatParticipant.of(1L, room);
        RealtimeChatParticipant seller = RealtimeChatParticipant.of(2L, room);

        given(chatParticipantRepository.findAllByRealtimeChatRoomIdIn(List.of(10L)))
                .willReturn(List.of(buyer, seller));

        given(unreadService.getUnread(10L, 1L)).willReturn(3);
        given(chatProvider.getUserNames(List.of(2L), RealtimeChatSenderType.SELLER))
                .willReturn(Map.of(2L, "판매자"));

        // when
        List<RealtimeChatRoomResponse> responses =
                chatRoomService.getChatRooms(1L, RealtimeChatSenderType.BUYER);

        // then
        assertThat(responses).hasSize(1);
        RealtimeChatRoomResponse response = responses.get(0);
        assertThat(response.unreadCount()).isEqualTo(3);
        assertThat(response.opponentName()).isEqualTo("판매자");
    }

    @Test
    void 채팅방_목록_조회_빈_리스트_테스트() {
        given(chatRoomRepository.findRoomsByUserId(1L)).willReturn(List.of());

        List<RealtimeChatRoomResponse> responses =
                chatRoomService.getChatRooms(1L, RealtimeChatSenderType.BUYER);

        assertThat(responses).isEmpty();
    }
}

