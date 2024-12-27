package com.yesy.team.build_devloper.security.jwt;

import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.repository.MemberRepository;
import com.yesy.team.build_devloper.security.custom.CustomOAuth2User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, MemberRepository memberRepository) {
        super(authenticationManager);
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.debug("Processing request URL: {}", request.getRequestURI());

        String accessToken = extractTokenFromCookies(request, "accessToken");

        if (accessToken == null) {
            log.debug("Access Token 쿠키가 없습니다. URL: {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.validateToken(accessToken);
            authenticateUser(request, claims);

        } catch (ExpiredJwtException e) {
            log.error("JWT 만료됨. URL: {}, Message: {}", request.getRequestURI(), e.getMessage());
            String refreshToken = extractTokenFromCookies(request, "refreshToken");

            if (refreshToken != null) {
                handleRefreshToken(refreshToken, response);
                return;
            } else {
                log.warn("Access Token 만료 및 Refresh Token 쿠키 누락. URL: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT expired and no refresh token provided");
                return;
            }
        } catch (JwtException e) {
            log.error("JWT 검증 실패. URL: {}, Message: {}", request.getRequestURI(), e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void authenticateUser(HttpServletRequest request, Claims claims) {
        String email = claims.getSubject();
        String googleLoginId = claims.get("googleLoginId", String.class);
        Long userId = claims.get("userId", Long.class); // JWT에서 userId 추출

        if (email != null && googleLoginId != null && userId != null) {
            Member user = new Member(); // DB 조회 없이 Member 객체 생성
            user.setId(userId); // JWT에서 추출한 userId 설정
            user.setEmail(email);
            user.setGoogleLoginId(googleLoginId);

            CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, null, null, null, false);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    customOAuth2User, null, customOAuth2User.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.info("JWT 인증 성공: {} (GoogleLoginId: {}, UserId: {}, URL: {})",
                    email, googleLoginId, userId, request.getRequestURI());
        } else {
            throw new JwtException("Invalid JWT payload");
        }
    }

    private void handleRefreshToken(String refreshToken, HttpServletResponse response) throws IOException {
        try {
            Claims refreshClaims = jwtUtil.validateToken(refreshToken);
            String email = refreshClaims.getSubject();
            String googleLoginId = refreshClaims.get("googleLoginId", String.class);
            Long userId = refreshClaims.get("userId", Long.class);
            boolean isNewUser = refreshClaims.get("isNewUser", Boolean.class);

            if (email == null || googleLoginId == null || userId == null) {
                log.warn("리프레시 토큰 검증 실패: 잘못된 토큰 데이터");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Refresh Token");
                return;
            }

            // 새로운 Access Token 생성
            String newAccessToken = jwtUtil.generateToken(googleLoginId, email, userId, isNewUser);

            // Access Token 쿠키에 저장
            Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(15 * 60); // 15분
            response.addCookie(accessTokenCookie);

            log.info("리프레시 토큰으로 새 Access Token 생성 및 쿠키에 저장: {}", newAccessToken);
        } catch (ExpiredJwtException e) {
            log.error("리프레시 토큰 만료됨: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh Token expired");
        } catch (JwtException e) {
            log.error("리프레시 토큰 검증 실패: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Refresh Token");
        }
    }
}
