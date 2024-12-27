package com.yesy.team.build_devloper.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MemberDto {
    private long userId;
    private String uName;
    private String nickname;
    private String email;
    private boolean alertEnabled;
    private LocalDateTime updateDate;
    private String googleLoginId;
}
