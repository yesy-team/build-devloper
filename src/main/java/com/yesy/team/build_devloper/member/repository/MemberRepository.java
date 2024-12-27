package com.yesy.team.build_devloper.member.repository;

import com.yesy.team.build_devloper.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByGoogleLoginId(String googleLoginId);
}
