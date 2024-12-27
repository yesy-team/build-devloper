package com.yesy.team.build_devloper.security.custom;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException, ServletException {
        System.err.println("======================== OAuth2 로그인 성공 ========================");

        // 기본 성공 핸들러 동작 확인
        super.onAuthenticationSuccess(req, res, authentication);

        // OAuth2 인증 성공 시 사용자 정보를 가져옴
        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        String email = oauthUser.getEmail(); // 사용자 이메일 가져오기
        System.err.println("로그인한 사용자 이메일: " + email);

        // 로그인 성공 후 리다이렉트 설정
        System.err.println("메인 페이지로 리다이렉트: /api/home/main");
        getRedirectStrategy().sendRedirect(req, res, "/api/home/main");
    }
}