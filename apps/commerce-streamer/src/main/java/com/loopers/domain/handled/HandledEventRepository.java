package com.loopers.domain.handled;

public interface HandledEventRepository {

    boolean existsByEventId(String eventId);

    HandledEvent save(HandledEvent handledEvent);
}
