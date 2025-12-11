package pproject.once_upon_a_time.global.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {
    // kakaoUserId가 제거됨
    private String name;
    private String gender;
    private String birth; // "YYYY.MM.DD"
    private String nickname;
    private String personalPhone;
}
