package com.yesy.team.build_devloper.security.custom;

import com.yesy.team.build_devloper.redis.service.RedisRefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;

public class CustomLogoutHandler implements LogoutHandler {

    @Autowired
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    @Autowired
    private final RedisRefreshTokenService redisRefreshTokenService; // Redis 서비스 추가

    // 생성자를 통해 주입
    @Autowired
    public CustomLogoutHandler(OAuth2AuthorizedClientRepository authorizedClientRepository,
                               @Lazy RedisRefreshTokenService redisRefreshTokenService) {
        this.authorizedClientRepository = authorizedClientRepository;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        System.err.println("로그아웃 핸들러 호출됨");

        try {
            if (authentication == null) {
                System.err.println("Authentication 객체가 null입니다. SecurityContext에서 강제 가져오기 시도.");
                authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null) {
                    System.err.println("SecurityContext에서도 인증 정보를 가져오지 못했습니다. 로그아웃 중단.");
                    return;
                }
            }

            // 사용자 이메일 가져오기
            String email = authentication.getName();
            if (email == null || email.isEmpty()) {
                System.err.println("Authentication name이 null 또는 비어있습니다. 로그아웃 중단.");
                return;
            }

            System.err.println("Authentication name: " + email);

            // Redis에서 Refresh Token 삭제
            if (redisRefreshTokenService != null) {
                redisRefreshTokenService.deleteRefreshToken(email);
                System.err.println("Redis에서 Refresh Token 삭제 완료");
            } else {
                System.err.println("RedisRefreshTokenService가 null입니다. Refresh Token 삭제 실패.");
            }

            // Google OAuth2 클라이언트 삭제
            if (authorizedClientRepository != null) {
                authorizedClientRepository.removeAuthorizedClient("google", authentication, request, response);
                System.err.println("Google 클라이언트 삭제 완료");
            }

            // 쿠키 삭제
            deleteCookie(response, "accessToken");
            deleteCookie(response, "refreshToken");
            System.err.println("쿠키 삭제 완료");

        } catch (Exception e) {
            System.err.println("로그아웃 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
        }

        System.err.println("로그아웃 처리 완료");
    }

    // 쿠키 삭제 메서드
    private void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 만료 처리
        response.addCookie(cookie);
    }
}