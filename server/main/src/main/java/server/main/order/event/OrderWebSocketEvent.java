package server.main.order.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderWebSocketEvent {
    private final Long tokenId;
    private final Long memberId;
    private final List<Long> counterMemberIds; // 체결 상대방 memberId 목록
}
