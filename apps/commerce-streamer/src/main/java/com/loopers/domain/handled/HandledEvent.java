package com.loopers.domain.handled;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

/**
 * 처리 완료된 이벤트 기록(멱등 처리용). event_id 를 PK 로 두어 중복 수신을 흡수한다.
 *
 * <p>로그 테이블과 분리하는 이유: 이 테이블은 "이미 처리했는가?"만 빠르게 판정하는 <b>제어용</b>이라
 * PK 조회/유니크 제약에 최적화되어야 하고, 원본 이벤트 로그(감사/재처리용, append-only, 대용량)와는
 * 접근 패턴·수명주기·인덱싱 요구가 다르기 때문이다.
 */
@Entity
@Table(name = "event_handled")
public class HandledEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    protected HandledEvent() {}

    private HandledEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.handledAt = ZonedDateTime.now();
    }

    public static HandledEvent of(String eventId, String eventType) {
        return new HandledEvent(eventId, eventType);
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public ZonedDateTime getHandledAt() { return handledAt; }
}
