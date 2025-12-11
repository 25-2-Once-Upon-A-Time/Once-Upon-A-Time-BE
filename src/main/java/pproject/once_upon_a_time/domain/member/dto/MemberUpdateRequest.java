package pproject.once_upon_a_time.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {

    @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
    private String nickname;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (010-xxxx-xxxx)")
    private String personalPhone;
}
