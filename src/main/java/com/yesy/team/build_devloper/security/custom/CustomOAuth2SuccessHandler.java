package com.yesy.team.build_devloper.security.custom;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        log.info("JWT 인증 성공: {}, UserId: {}, isNewUser: {}", customOAuth2User.getEmail(), customOAuth2User.getUserId(), customOAuth2User.isNewUser());

        // Access Token 쿠키에 저장
        Cookie accessTokenCookie = new Cookie("accessToken", customOAuth2User.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // HTTPS 환경에서만 동작
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(15 * 60); // 15분

        response.addCookie(accessTokenCookie);

        // 리디렉션 처리
        String redirectUrl = customOAuth2User.isNewUser() ? "/react/map" : "/react/dashboard";
        response.sendRedirect(redirectUrl);
    }
}
