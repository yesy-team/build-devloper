package com.yesy.team.build_devloper.redis.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 캐시 목록별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 별자리 데이터 캐시 - TTL: 30분
        cacheConfigurations.put("constellations",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));

        // 행성 가시성 데이터 캐시 - TTL: 10분
        cacheConfigurations.put("planetVisibility",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));

        //유성우 가시성 데이터 캐시 - TTL: 7일
        cacheConfigurations.put("meteorShowerVisibility",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7)));

        // 기타 캐시 데이터 - TTL: 1시간
        cacheConfigurations.put("generalData",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));

        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // 기본 TTL
                .disableCachingNullValues();

        // RedisCacheManager 빌드
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig) // 기본 설정
                .withInitialCacheConfigurations(cacheConfigurations) // 캐시별 설정
                .build();
    }
}
