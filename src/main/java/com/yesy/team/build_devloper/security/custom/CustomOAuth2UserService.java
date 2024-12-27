package com.yesy.team.build_devloper.security.custom;

import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.repository.MemberRepository;
import com.yesy.team.build_devloper.redis.service.RedisRefreshTokenService;
import com.yesy.team.build_devloper.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;


@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public CustomOAuth2UserService(MemberRepository memberRepository, JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService) {
        this.memberRepository = memberRepository;
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.err.println("================================로그인 DB 접근로직 시작.=================================");
        // 기본적으로 OAuth2User 정보를 가져옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // OAuth 제공자가 Google인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email;
        String googleLoginId;
        String profileImageUrl;

        if ("google".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            googleLoginId = oAuth2User.getAttribute("sub"); // Google ID
            profileImageUrl = oAuth2User.getAttribute("picture"); // Google 프로필 이미지 URL
            System.out.println("Google OAuth2로 가져온 사용자 이메일: " + email);
            System.out.println("Google OAuth2로 가져온 사용자 ID: " + googleLoginId);

            // 세션에 프로필 이미지 URL 저장
            storeProfileImageInSession(profileImageUrl);

            return processGoogleUser(oAuth2User, email, googleLoginId);
        } else {
            throw new OAuth2AuthenticationException("Unknown registrationId: " + registrationId);
        }
    }

    // 세션에 프로필 이미지 URL 저장하는 메서드
    private void storeProfileImageInSession(String profileImageUrl) {
        // 현재 세션을 가져옴
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession();

        // 세션에 프로필 이미지 URL을 저장
        session.setAttribute("profileImageUrl", profileImageUrl);
    }

    // Google 사용자 처리
    public CustomOAuth2User processGoogleUser(OAuth2User oAuth2User, String email, String googleLoginId) {
        // Google ID로 기존 사용자 확인
        Optional<Member> localUser = memberRepository.findByGoogleLoginId(googleLoginId);
        boolean isNewUser;

        Member user;
        if (localUser.isPresent()) {
            // 기존 사용자
            user = localUser.get();
            isNewUser = false; // 기존 사용자이므로 false
            log.info("기존 사용자 확인됨: {}, isNewUser: {}", email, isNewUser);
        } else {
            // 새 사용자 등록
            isNewUser = true;
            String displayName = oAuth2User.getAttribute("name") != null ? oAuth2User.getAttribute("name") : "사용자";
            String nickname = email.split("@")[0];

            user = new Member();
            user.setEmail(email);
            user.setUName(displayName);
            user.setNickname(nickname);
            user.setGoogleLoginId(googleLoginId);

            memberRepository.save(user);
            log.info("로컬 DB에 새 Google 사용자 저장 완료: {}, isNewUser: {}", email, isNewUser);
        }

        // JWT 발급
        String accessToken = jwtUtil.generateToken(user.getGoogleLoginId(), user.getEmail(), user.getId(), isNewUser);
        String refreshToken = jwtUtil.generateRefreshToken(user.getGoogleLoginId(), user.getEmail(), user.getId());

        // Refresh Token Redis에 저장
        redisRefreshTokenService.saveRefreshToken(user.getEmail(), refreshToken);
        log.info("Successfully Redis Save Token: {}", refreshToken);

        // 최종 isNewUser 값 확인
        log.info("processGoogleUser 완료. email: {}, googleLoginId: {}, isNewUser: {}", email, googleLoginId, isNewUser);

        // CustomOAuth2User 반환
        return new CustomOAuth2User(user, oAuth2User.getAttributes(), accessToken, refreshToken, isNewUser);
    }


    // 로컬 DB에 Google 사용자 생성
    private void createGoogleUserInLocalDB(String email, String uName, String nickname, String googleLoginId) {
        if (memberRepository.findByGoogleLoginId(googleLoginId).isPresent()) {
            System.out.println("이미 존재하는 Google 사용자: " + googleLoginId);
            return;
        }

        Member newUser = new Member();
        newUser.setEmail(email);
        newUser.setUName(uName != null ? uName : "사용자");
        newUser.setNickname(nickname);
        newUser.setGoogleLoginId(googleLoginId);

        memberRepository.save(newUser);
        System.out.println("로컬 DB에 Google 사용자 저장 완료: " + email);
    }
}
