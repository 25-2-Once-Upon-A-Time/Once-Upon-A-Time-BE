package pproject.once_upon_a_time.domain.member.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.member.domain.Member;

import java.time.LocalDate;

@Getter
public class MemberResponse {

    private final String name;
    private final String nickname;
    private final String gender;
    private final LocalDate birth;
    private final String personalPhone;

    @Builder
    public MemberResponse(String name, String nickname, String gender, LocalDate birth, String personalPhone) {
        this.name = name;
        this.nickname = nickname;
        this.gender = gender;
        this.birth = birth;
        this.personalPhone = personalPhone;
    }

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .gender(member.getGender())
                .birth(member.getBirth())
                .personalPhone(member.getPersonalPhone())
                .build();
    }
}
