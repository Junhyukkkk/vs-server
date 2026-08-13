package com.ject.vs.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ject.vs.ai.port.in.AiInsightUseCase.AiInsightResult;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase.HotTopicResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, AiInsightResult> personalizedAiInsightCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(24))
                .build();
    }

    /**
     * 핫토픽 순위 캐시. 단일 키만 담는다.
     *
     * <p>정상적으로는 3시간 주기 스케줄러가 먼저 덮어쓰므로 TTL이 만료될 일이 없다.
     * 4시간 TTL은 스케줄러가 멈췄을 때 오래된 순위가 무한정 노출되는 것을 막는 안전망이다.
     */
    @Bean
    public Cache<String, HotTopicResult> hotTopicCache() {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofHours(4))
                .build();
    }
}
