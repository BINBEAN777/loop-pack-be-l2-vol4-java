package com.loopers.testconfig;

import org.springframework.context.annotation.Configuration;

/**
 * 테스트 전역에서 토큰 발급 스케줄러의 자동 실행을 사실상 끈다.
 *
 * 이유: Testcontainers Redis 는 JVM 전체에서 하나를 공유하고, Spring 은 테스트 컨텍스트를
 * 캐싱해 살려둔다. 그래서 스케줄러가 있는 어떤 컨텍스트든 100ms 마다 공유 Redis 의 큐를
 * 비워, 큐 상태를 검증하는 다른 테스트를 오염시킨다. interval 을 크게 잡아 자동 실행을 막고,
 * 스케줄러 자체 테스트는 issueTokens() 를 직접 호출한다.
 */
@Configuration
public class SchedulerTestConfig {
    static {
        System.setProperty("loopers.queue.scheduler.interval-ms", "3600000");
    }
}
