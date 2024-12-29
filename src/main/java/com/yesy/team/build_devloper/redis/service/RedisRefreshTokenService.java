package com.yesy.team.build_devloper.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisRefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Refresh Token 저장
    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set("refreshToken:" + email, refreshToken, 7, TimeUnit.DAYS); // 7일 만료
    }

    // Refresh Token 가져오기
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get("refreshToken:" + email);
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String email, String refreshToken) {
        String storedToken = redisTemplate.opsForValue().get(email);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    // Refresh Token 삭제
    public void deleteRefreshToken(String email) {
        log.info("Attempting to delete Refresh Token for email: {}", email);
        if (redisTemplate == null) {
            log.error("RedisTemplate is null!");
            return;
        }
        Boolean isDeleted = redisTemplate.delete(email);
        log.info("Refresh Token deleted: {}", isDeleted);
    }
}
