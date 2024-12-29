package com.yesy.team.build_devloper.member.service;

import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MemberService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // OAuth2 제공자 (google, github 등)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String loginId;
        String email;
        String name;

        // Google 또는 GitHub 정보 추출
        if ("google".equals(provider)) {
            loginId = oAuth2User.getAttribute("sub"); // Google ID
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if ("github".equals(provider)) {
            loginId = oAuth2User.getAttribute("id").toString(); // GitHub ID
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        // DB에서 사용자 검색
        Optional<Member> memberOptional = memberRepository.findByLoginIdAndProvider(loginId, provider);

        Member member;
        if (memberOptional.isPresent()) {
            // 기존 사용자
            member = memberOptional.get();
        } else {
            // 새 사용자 등록
            member = new Member();
            member.setLoginId(loginId);
            member.setProvider(provider); // Google 또는 GitHub
        }

        // 사용자 정보 업데이트
        member.setEmail(email);
        member.setUName(name);
        memberRepository.save(member);

        // OAuth2User 반환
        return new DefaultOAuth2User(
                Collections.singletonList(() -> "ROLE_USER"),
                oAuth2User.getAttributes(),
                "name"
        );
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }
}

