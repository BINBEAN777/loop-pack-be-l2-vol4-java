package com.loopers.domain.outbox;

import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    /** 아직 발행되지 않은(PENDING) outbox 를 id 오름차순(발생 순서)으로 조회한다. */
    List<OutboxEvent> findPending(int limit);
}
