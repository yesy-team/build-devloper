package com.yesy.team.build_devloper.redis.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig implements SmartLifecycle {

    private boolean isRunning = false;

    private JedisConnectionFactory jedisConnectionFactory;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        System.err.println("Initializing RedisConnectionFactory with Jedis...");

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);

        jedisConnectionFactory = new JedisConnectionFactory(redisConfig);
        jedisConnectionFactory.afterPropertiesSet(); // 초기화 확인
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Override
    public void start() {
        isRunning = true;
        System.out.println("RedisConfig with Jedis started.");
    }

    @Override
    public void stop() {
        isRunning = false;
        System.out.println("Stopping Redis connections...");
        if (jedisConnectionFactory != null) {
            jedisConnectionFactory.destroy();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // 다른 Bean들보다 나중에 실행
    }

    @PreDestroy
    public void closeRedisConnection() {
        if (jedisConnectionFactory != null) {
            System.err.println("Closing JedisConnectionFactory...");
            jedisConnectionFactory.destroy();
        }
    }
}
