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

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<Member> memberOptional = memberRepository.findByGoogleLoginId(googleId);

        Member member;
        if (memberOptional.isPresent()) {
            member = memberOptional.get();
        } else {
            member = new Member();
            member.setGoogleLoginId(googleId);
        }
        member.setEmail(email);
        member.setUName(name);
        memberRepository.save(member);

        return new DefaultOAuth2User(
                Collections.singletonList(() -> "ROLE_USER"),
                oAuth2User.getAttributes(),
                "name"
        );
    }

    public Optional<Member> findByEmail(Object sub) {
        return memberRepository.findByEmail(sub.toString());
    }
}
