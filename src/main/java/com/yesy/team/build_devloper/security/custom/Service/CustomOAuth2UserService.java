package com.yesy.team.build_devloper.security.custom.Service;

import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.repository.MemberRepository;
import com.yesy.team.build_devloper.redis.service.RedisRefreshTokenService;
import com.yesy.team.build_devloper.security.custom.CustomOAuth2User;
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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email;
        String loginId;
        String profileImageUrl;

        if ("google".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            loginId = oAuth2User.getAttribute("sub"); // Google의 고유 ID
            profileImageUrl = oAuth2User.getAttribute("picture");

            System.out.println("Google OAuth2로 가져온 사용자 이메일: " + email);
            System.out.println("Google OAuth2로 가져온 사용자 ID: " + loginId);
        } else if ("github".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            loginId = oAuth2User.getAttribute("id").toString(); // GitHub의 고유 ID
            profileImageUrl = oAuth2User.getAttribute("avatar_url");

            System.out.println("GitHub OAuth2로 가져온 사용자 이메일: " + email);
            System.out.println("GitHub OAuth2로 가져온 사용자 ID: " + loginId);
        } else {
            throw new OAuth2AuthenticationException("Unknown registrationId: " + registrationId);
        }

        storeProfileImageInSession(profileImageUrl);
        return processOAuth2User(oAuth2User, email, loginId, registrationId);
    }

    // 세션에 프로필 이미지 URL 저장하는 메서드
    private void storeProfileImageInSession(String profileImageUrl) {
        // 현재 세션을 가져옴
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession();

        // 세션에 프로필 이미지 URL을 저장
        session.setAttribute("profileImageUrl", profileImageUrl);
    }

    public CustomOAuth2User processOAuth2User(
            OAuth2User oAuth2User,
            String email,
            String loginId,
            String provider) {

        // DB에서 사용자 검색
        Optional<Member> localUser = memberRepository.findByEmail(email); // loginId는 Google과 GitHub 공통 ID
        boolean isNewUser;

        Member user;
        if (localUser.isPresent()) {
            // 기존 사용자
            user = localUser.get();
            isNewUser = false;
            log.info("기존 {} 사용자 확인됨: {}, isNewUser: {}", provider, email, isNewUser);
        } else {
            // 새 사용자 등록
            isNewUser = true;
            String displayName = oAuth2User.getAttribute("name") != null ? oAuth2User.getAttribute("name") : "사용자";
            String nickname = email.split("@")[0];

            user = new Member();
            user.setEmail(email);
            user.setUName(displayName);
            user.setNickname(nickname);
            user.setLoginId(loginId); // Google, GitHub 모두 loginId로 저장
            user.setProvider(provider);

            memberRepository.save(user);
            log.info("로컬 DB에 새 {} 사용자 저장 완료: {}, isNewUser: {}", provider, email, isNewUser);
        }

        // JWT 발급
        String accessToken = jwtUtil.generateToken(user.getLoginId(), user.getEmail(), user.getId(), isNewUser);
        String refreshToken = jwtUtil.generateRefreshToken(user.getLoginId(), user.getEmail(), user.getId());

        // Refresh Token Redis에 저장
        redisRefreshTokenService.saveRefreshToken(user.getEmail(), refreshToken);
        log.info("Successfully Redis Save Token for {}: {}", provider, refreshToken);

        return new CustomOAuth2User(user, oAuth2User.getAttributes(), accessToken, refreshToken, isNewUser);
    }
    // 로컬 DB에 Google 사용자 생성
//    private void createGoogleUserInLocalDB(String email, String uName, String nickname, String loginId) {
//        if (memberRepository.findByGoogleLoginId(loginId).isPresent()) {
//            System.out.println("이미 존재하는 Google 사용자: " + loginId);
//            return;
//        }
//
//        Member newUser = new Member();
//        newUser.setEmail(email);
//        newUser.setUName(uName != null ? uName : "사용자");
//        newUser.setNickname(nickname);
//        newUser.setLoginId(loginId);
//
//        memberRepository.save(newUser);
//        System.out.println("로컬 DB에 Google 사용자 저장 완료: " + email);
//    }
}
