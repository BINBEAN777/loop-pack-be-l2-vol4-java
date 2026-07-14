package com.loopers.testconfig;

import org.springframework.context.annotation.Configuration;

/**
 * 테스트 전역에서 토큰 발급 스케줄러 빈 자체를 생성하지 않는다.
 *
 * 이유: Testcontainers Redis 는 JVM 전체에서 하나를 공유하고, Spring 은 테스트 컨텍스트를
 * 캐싱해 살려둔다. 스케줄러가 살아있으면 어떤 컨텍스트에서든 공유 Redis 의 큐를 팝해
 * 다른 테스트를 오염시킨다. 빈을 아예 만들지 않아 팝할 주체를 제거한다.
 * 스케줄러 자체 테스트만 @TestPropertySource(enabled=true) 로 다시 켜서 issueTokens() 를 직접 호출한다.
 */
@Configuration
public class SchedulerTestConfig {
    static {
        System.setProperty("loopers.queue.scheduler.enabled", "false");
    }
}
