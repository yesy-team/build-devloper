package com.yesy.team.build_devloper.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.expiration}")
    private long expirationTime;

    @Value("${spring.jwt.refresh-expiration}")
    private long refreshExpirationTime;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String sub, String email, Long userId, boolean isNewUser) {
        return Jwts.builder()
                .setSubject(email) // 이메일을 Subject로 설정
                .claim("loginId", sub) // Google sub를 id처럼 사용
                .claim("userId", userId) // userId 추가
                .claim("isNewUser", isNewUser)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String sub, String email, Long userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("loginId", sub)
                .claim("userId", userId) // userId 추가
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(null, null, "JWT expired");
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT");
        }
    }
}