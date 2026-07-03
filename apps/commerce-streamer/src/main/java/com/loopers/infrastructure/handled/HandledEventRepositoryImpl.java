package com.loopers.infrastructure.handled;

import com.loopers.domain.handled.HandledEvent;
import com.loopers.domain.handled.HandledEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class HandledEventRepositoryImpl implements HandledEventRepository {

    private final HandledEventJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public HandledEvent save(HandledEvent handledEvent) {
        return jpaRepository.save(handledEvent);
    }
}
