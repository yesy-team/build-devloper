package com.yesy.team.build_devloper.member.mapper;

import com.yesy.team.build_devloper.member.dto.MemberDto;
import com.yesy.team.build_devloper.member.entity.Member;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    MemberDto toMemberDto(Member member);
}
