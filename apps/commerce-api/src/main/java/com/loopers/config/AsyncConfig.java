package com.loopers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 도메인 이벤트 처리용 비동기 executor.
 *
 * <p>단일 스레드(core=max=1) + FIFO 큐로 <b>이벤트 처리 순서를 보장</b>한다.
 * 같은 상품에 대한 좋아요 추가/취소가 뒤바뀌면 집계가 틀어질 수 있어(감소 가드 like_count &gt; 0),
 * 순서를 지키는 것이 중요하다. (Step 2 Kafka 의 PartitionKey 순서 보장과 같은 맥락)
 */
@Configuration
public class AsyncConfig {

    @Bean("eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
