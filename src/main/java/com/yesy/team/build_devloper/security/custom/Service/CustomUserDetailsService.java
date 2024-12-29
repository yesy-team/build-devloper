package com.yesy.team.build_devloper.security.custom.Service;


import com.yesy.team.build_devloper.member.entity.Member;
import com.yesy.team.build_devloper.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User email 찾을 수 없음!: " + email));
        return new org.springframework.security.core.userdetails.User(member.getEmail(), "", List.of(() -> "ROLE_USER"));
    }
}