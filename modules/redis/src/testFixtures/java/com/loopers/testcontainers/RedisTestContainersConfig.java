package com.loopers.testcontainers;

import com.redis.testcontainers.RedisContainer;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class RedisTestContainersConfig {
    private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"));

    // 시스템 프로퍼티 세팅은 반드시 static 블록에서 — 생성자에서 하면 다른 @Configuration(RedisConfig)의
    // 프로퍼티 바인딩이 먼저 일어나는 경우 localhost 기본값으로 바인딩된다. (빈 생성 순서는 앱마다 다르다)
    static {
        redisContainer.start();
        System.setProperty("datasource.redis.database", "0");
        System.setProperty("datasource.redis.master.host", redisContainer.getHost());
        System.setProperty("datasource.redis.master.port", String.valueOf(redisContainer.getFirstMappedPort()));
        System.setProperty("datasource.redis.replicas[0].host", redisContainer.getHost());
        System.setProperty("datasource.redis.replicas[0].port", String.valueOf(redisContainer.getFirstMappedPort()));
    }
}
