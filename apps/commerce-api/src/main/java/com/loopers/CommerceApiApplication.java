package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@EnableFeignClients                 // @FeignClient 인터페이스 스캔 (PG 호출)
@EnableScheduling                   // @Scheduled 복구 작업 활성화
@EnableAsync                        // @Async 이벤트 리스너(좋아요 집계 등) 활성화
@ConfigurationPropertiesScan
@SpringBootApplication
public class CommerceApiApplication {

    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CommerceApiApplication.class, args);
    }
}
