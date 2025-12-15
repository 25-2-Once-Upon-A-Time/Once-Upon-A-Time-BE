package pproject.once_upon_a_time.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.common.MemberRole;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminMemberResponseDto {
    private Long memberId;
    private String name;
    private String nickname;
    private String kakaoUserId;
    private MemberRole role;
    private LocalDateTime joinDate; // 가입일 추가

    public static AdminMemberResponseDto from(Member member) {
        return AdminMemberResponseDto.builder()
            .memberId(member.getId())
            .name(member.getName())
            .nickname(member.getNickname())
            .kakaoUserId(member.getKakaoUserId())
            .role(member.getRole())
            .joinDate(member.getCreatedDate())
            .build();
    }
}
