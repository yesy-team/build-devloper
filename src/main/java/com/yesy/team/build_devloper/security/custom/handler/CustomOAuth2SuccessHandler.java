package com.yesy.team.build_devloper.security.custom.handler;

import com.yesy.team.build_devloper.security.custom.CustomOAuth2User;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${spring.application.frontend.url}")
    private String frontendUrl; // 프론트엔드 URL

    @PostConstruct
    public void checkFrontendUrl() {
        System.err.println("Frontend URL during initialization: " + frontendUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        System.err.println("Frontend URL in SuccessHandler: " + frontendUrl);

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        log.info("JWT 인증 성공: {}, UserId: {}, isNewUser: {}", customOAuth2User.getEmail(), customOAuth2User.getUserId(), customOAuth2User.isNewUser());

        // Access Token 쿠키에 저장
        Cookie accessTokenCookie = new Cookie("accessToken", customOAuth2User.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // HTTPS 환경에서만 동작
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(15 * 60); // 15분
        response.addCookie(accessTokenCookie);

        // 프론트엔드로 리디렉션
        String redirectUrl = frontendUrl + "/main";;
        System.err.println("redirectUrl: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}