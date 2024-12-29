package com.yesy.team.build_devloper.member.controller;

import com.yesy.team.build_devloper.member.dto.MemberDto;
import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.mapper.MemberMapper;
import com.yesy.team.build_devloper.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberMapper memberMapper;


    @GetMapping("/me")
    public ResponseEntity<MemberDto> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

                Optional<Member> memberOptional = memberService.findByEmail(Objects.requireNonNull(oAuth2User.getAttribute("email")));
                if (memberOptional.isPresent()) {
                    Member member = memberOptional.get();
                    MemberDto memberDto = memberMapper.toMemberDto(member);
                    return ResponseEntity.ok(memberDto);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    // 로그인 상태 확인 API
    @GetMapping("/check-auth")
    public boolean checkAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
