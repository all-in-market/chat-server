package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.RealtimeChatRoomResponse;
import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeChatRoomService {
    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RealtimeChatParticipantRepository chatParticipantRepository;
    private final RedisUnreadService unreadService;
    private final RealtimeChatProvider chatProvider;

    @Transactional
    public RealtimeChatRoom getOrCreateRoom(Long buyerId, Long sellerId) {
        // 기존 방 조회
        return chatRoomRepository.findByBuyerIdAndSellerId(buyerId, sellerId).orElseGet(
                () -> createRoomSafely(buyerId, sellerId)
        );
    }

    public List<RealtimeChatRoomResponse> getChatRooms(Long userId, RealtimeChatSenderType senderType) {
        // 내가 속한 채팅방 조회
        List<RealtimeChatRoom> rooms = chatRoomRepository.findRoomsByUserId(userId);

        if (rooms.isEmpty()) return List.of();

        List<Long> roomIds = rooms.stream().map(RealtimeChatRoom::getId).toList();

        // N+1 해결을 위한 채팅방들의 모든 참여자 정보를 한번에 조회
        List<RealtimeChatParticipant> allParticipants = chatParticipantRepository.findAllByRealtimeChatRoomIdIn(roomIds);

        // 채팅방 ID 별로 참여자 그룹화
        Map<Long, List<RealtimeChatParticipant>> participantMap = allParticipants.stream()
                .collect(Collectors.groupingBy(p -> p.getRealtimeChatRoom().getId()));

        // 성능 최적화를 위한 상대방 이름 한번에 조회
        Map<Long, String> opponentMap = getOpponentNameMap(roomIds, userId, senderType, participantMap);

        return rooms.stream()
                .map(room -> {
                    // unread count (Redis)
                    int unreadCount = unreadService.getUnread(room.getId(), userId);

                    // 해당 방의 참여자 중 나를 제외한 첫 번째 사람을 상대방으로 간주
                    Long opponentId = participantMap.getOrDefault(room.getId(), List.of()).stream()
                            .filter(p -> !p.getUserId().equals(userId))
                            .map(RealtimeChatParticipant::getUserId)
                            .findFirst()
                            .orElse(null);

                    return new RealtimeChatRoomResponse(
                            room.getId(),
                            room.getRoomName(),
                            room.getLastMessage() == null ? "" : room.getLastMessage(),
                            room.getLastMessageTime(),
                            unreadCount,
                            opponentMap.getOrDefault(opponentId, "알 수 없음")
                    );
                })
                .toList();
    }

    private Map<Long, String> getOpponentNameMap(
            List<Long> roomIds,
            Long userId,
            RealtimeChatSenderType senderType,
            Map<Long, List<RealtimeChatParticipant>> participantsByRoom
    ) {

        // 상대방 정보 조회를 위한 모든 상대방 ID 추출
        List<Long> opponentIds = participantsByRoom.values().stream()
                .flatMap(List::stream)
                .filter(p -> !p.getUserId().equals(userId))
                .map(RealtimeChatParticipant::getUserId)
                .distinct()
                .toList();

        // 상대방 타입 결정 (내가 구매자면 상대방은 판매자)
        RealtimeChatSenderType opponentType = (senderType == RealtimeChatSenderType.BUYER)
                ? RealtimeChatSenderType.SELLER : RealtimeChatSenderType.BUYER;

        // 이름 일괄 조회
        return chatProvider.getUserNames(opponentIds, opponentType);
    }

    private RealtimeChatRoom createRoomSafely(Long buyerId, Long sellerId) {
        try {
            // 채팅방 생성
            RealtimeChatRoom chatRoom = chatRoomRepository.save(
                    RealtimeChatRoom.of(buyerId, sellerId)
            );

            // 참여자 2명 등록
            chatParticipantRepository.save(RealtimeChatParticipant.of(buyerId, chatRoom));

            chatParticipantRepository.save(RealtimeChatParticipant.of(sellerId, chatRoom));

            return chatRoom;

        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 이미 생성된 경우
            return chatRoomRepository.findByBuyerIdAndSellerId(buyerId, sellerId).orElseThrow(
                    () -> new BaseException(ErrorEnum.CHAT_ROOM_ALREADY_EXISTS)
            );
        }
    }
}
