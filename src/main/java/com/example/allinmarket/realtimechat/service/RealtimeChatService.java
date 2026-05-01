package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.realtimechat.dto.*;
import com.example.allinmarket.realtimechat.entity.RealtimeChatMessage;
import com.example.allinmarket.realtimechat.entity.RealtimeChatParticipant;
import com.example.allinmarket.realtimechat.entity.RealtimeChatRoom;
import com.example.allinmarket.realtimechat.entity.RealtimeReadStatus;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.realtimechat.repository.RealtimeChatMessageRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatParticipantRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeChatRoomRepository;
import com.example.allinmarket.realtimechat.repository.RealtimeReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeChatService {
    private final RealtimeChatMessageRepository chatMessageRepository;
    private final RealtimeReadStatusRepository readStatusRepository;
    private final RealtimeChatParticipantRepository chatParticipantRepository;
    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RedisUnreadService unreadService;
    private final RedisPublisher redisPublisher;
    private final RealtimeChatProvider chatProvider;

    public RealtimeChatMessage save(RealtimeChatMessageDto dto, Long userId) {
        RealtimeChatMessage chatMessage = RealtimeChatMessage.of(
                dto.roomId(),
                userId,
                dto.message()
        );

        return chatMessageRepository.save(chatMessage);
    }

    @Transactional
    public void read(Long roomId, Long userId, Long messageId) {
        validateParticipant(roomId, userId);

        RealtimeReadStatus readStatus = readStatusRepository.findByRoomIdAndUserId(roomId, userId).orElseGet(
                () -> RealtimeReadStatus.of(roomId, userId, 0L)
        );

        readStatus.updateLastRead(messageId);

        readStatusRepository.save(readStatus);

        unreadService.resetUnread(roomId, userId);

        redisPublisher.publishRead(
                roomId,
                new RealtimeReadDto(roomId, userId, messageId)
        );
    }

    public List<Long> getParticipantIds(Long roomId) {
        return chatParticipantRepository.findUserIdsByRoomId(roomId);
    }

    @Transactional
    public void enterRoom(Long roomId, Long userId) {
        boolean exists = chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(roomId, userId);

        if (exists) return;

        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );

        RealtimeChatParticipant chatParticipant = RealtimeChatParticipant.of(
                userId,
                chatRoom
        );

        chatParticipantRepository.save(chatParticipant);
    }

    public void validateParticipant(Long roomId, Long userId) {
        boolean exists = chatParticipantRepository.existsByRealtimeChatRoomIdAndUserId(roomId, userId);

        if (!exists) {
            throw new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }

    public RealtimeChatHistoryResponse getChatHistory(Long roomId, Long userId, Long lastMessageId, int size) {
        validateParticipant(roomId, userId);

        List<RealtimeChatMessage> messages = chatMessageRepository.findMessages(
                roomId,
                lastMessageId,
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = messages.size() > size;

        if (hasNext) {
            messages = messages.subList(0, size);
        }

        // 정렬 전 원본 리스트(내림차순)에서 마지막 요소 = 다음 커서
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        List<RealtimeChatMessageResponse> responses = messages.stream()
                .map(RealtimeChatMessageResponse::from)
                .sorted(Comparator.comparing(RealtimeChatMessageResponse::messageId))
                .toList();

        return new RealtimeChatHistoryResponse(responses, nextCursor, hasNext);
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

    @Transactional
    public void updateLastMessage(Long roomId, String lastMessage, LocalDateTime lastMessageTime) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_NOT_FOUND)
        );

        chatRoom.updateLastMessage(lastMessage, lastMessageTime);
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
}
