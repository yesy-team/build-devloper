package com.yesy.team.build_devloper.member.controller;

import com.yesy.team.build_devloper.member.service.MemberService;
import com.yesy.team.build_devloper.redis.service.RedisRefreshTokenService;
import com.yesy.team.build_devloper.security.custom.CustomOAuth2User;
import com.yesy.team.build_devloper.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final JwtUtil jwtUtil;

//    @PostMapping("/refresh")
//    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, Object> payload) {
//        log.debug("=============================리프레시 토큰 로직 시작====================================");
//
//        try {
//            // 클라이언트로부터 사용자 식별 정보 (userId) 수신
//            Long userId = Long.valueOf(payload.get("userId").toString());
//            log.debug("수신된 userId: {}", userId);
//
//            // userId로 이메일 가져오기
//            String email = memberService.getEmailByUserId(userId);
//            if (email == null) {
//                log.error("해당 userId로 이메일을 찾을 수 없습니다. userId: {}", userId);
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
//            }
//            log.debug("userId로 가져온 이메일: {}", email);
//
//            // Redis에서 해당 이메일의 리프레시 토큰 가져오기
//            String refreshToken = redisRefreshTokenService.getRefreshToken(email);
//            if (refreshToken == null) {
//                log.error("Redis에서 리프레시 토큰을 찾을 수 없습니다. email: {}", email);
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token not found");
//            }
//
//            // 리프레시 토큰 검증
//            Claims claims = jwtUtil.validateToken(refreshToken);
//            String googleLoginId = claims.get("googleLoginId", String.class);
//            log.debug("리프레시 토큰 검증 성공. email={}, googleLoginId={}", email, googleLoginId);
//
//            // 새 액세스 토큰 생성
//            String newAccessToken = jwtUtil.generateToken(googleLoginId, email, userId, false);
//            log.debug("새로 생성된 Access Token: {}", newAccessToken);
//
//            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
//        } catch (Exception e) {
//            log.error("리프레시 토큰 처리 중 예외 발생", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing refresh token");
//        }
//    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessTokenFromCookie(HttpServletRequest request, HttpServletResponse response) {
        log.debug("=============== 쿠키 기반 리프레시 토큰 로직 시작 ===============");

        try {
            // Access Token 쿠키 추출
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                log.error("쿠키가 존재하지 않습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No cookies found");
            }

            String accessToken = null;
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }

            if (accessToken == null) {
                log.error("Access Token 쿠키가 없습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access token cookie not found");
            }

            // Access Token 검증
            Claims accessClaims = jwtUtil.validateToken(accessToken);
            String email = accessClaims.getSubject();
            if (email == null) {
                log.error("Access Token에서 이메일을 추출하지 못했습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Access Token");
            }
            log.debug("Access Token 검증 성공. 이메일: {}", email);

            // Redis에서 리프레시 토큰 가져오기
            String refreshToken = redisRefreshTokenService.getRefreshToken(email);
            if (refreshToken == null) {
                log.error("Redis에서 리프레시 토큰을 찾을 수 없습니다. email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found in Redis");
            }

            // 리프레시 토큰 검증
            Claims refreshClaims = jwtUtil.validateToken(refreshToken);
            String googleLoginId = refreshClaims.get("googleLoginId", String.class);
            Long userId = refreshClaims.get("userId", Long.class);
            log.debug("리프레시 토큰 검증 성공. email={}, googleLoginId={}, userId={}", email, googleLoginId, userId);

            // 새 Access Token 생성
            String newAccessToken = jwtUtil.generateToken(googleLoginId, email, userId, false);
            log.debug("새로 생성된 Access Token: {}", newAccessToken);

            // 새로운 Access Token을 쿠키로 전달
            Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(15 * 60); // 15분
            response.addCookie(accessTokenCookie);

            return ResponseEntity.ok("Access Token 갱신 성공");
        } catch (Exception e) {
            log.error("쿠키 기반 리프레시 토큰 처리 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing refresh token");
        }
    }


    @GetMapping("/check")
    public ResponseEntity<?> checkAuthentication(HttpServletRequest request) {
        try {
            // Access Token 검증 (JwtAuthenticationFilter가 이미 처리)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
            boolean isNewUser = user.isNewUser();
            Long userId = user.getUserId(); // 사용자 ID 가져오기
            String email = user.getEmail();

            // 사용자 상태 반환
            return ResponseEntity.ok(Map.of(
                    "isAuthenticated", true,
                    "userId", userId,
                    "userInfo", Map.of("email", email, "isNewUser", isNewUser)
            ));
        } catch (Exception e) {
            log.error("Authentication check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred");
        }
    }

    @GetMapping("/auth/access-token")
    public ResponseEntity<?> getAccessToken(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(
                    user.getGoogleLoginId(),
                    user.getEmail(),
                    user.getUserId(),
                    false
            );

            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            log.error("Access Token 요청 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred");
        }
    }
}
