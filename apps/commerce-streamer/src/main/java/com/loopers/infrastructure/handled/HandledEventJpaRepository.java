package com.loopers.infrastructure.handled;

import com.loopers.domain.handled.HandledEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandledEventJpaRepository extends JpaRepository<HandledEvent, String> {
}
