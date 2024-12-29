package com.yesy.team.build_devloper.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String email; // JWT에서 추출한 사용자 이메일
    private final String googleLoginId; // JWT에서 추출한 Google Login ID
    private final Long userId; // DB에서 조회한 사용자 고유 ID 추가

    public JwtAuthenticationToken(String email, String googleLoginId, Long userId) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))); // 기본 권한 설정
        this.email = email;
        this.googleLoginId = googleLoginId;
        this.userId = userId;
        setAuthenticated(true); // 인증된 상태로 설정
    }

    public JwtAuthenticationToken(String email, Collection<? extends GrantedAuthority> authorities, String googleLoginId, Long userId) {
        super(authorities != null ? authorities : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))); // 기본 권한 설정
        this.email = email;
        this.googleLoginId = googleLoginId;
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // JWT 기반 인증이므로 credentials는 필요 없음
    }

    @Override
    public Object getPrincipal() {
        return email; // 이메일을 Principal로 사용
    }

    public String getEmail() {
        return this.email; // 이메일 반환 메서드 추가
    }

    public String getGoogleLoginId() {
        return this.googleLoginId; // Google Login ID 반환
    }

    public Long getUserId() {
        return this.userId; // User ID 반환
    }
}