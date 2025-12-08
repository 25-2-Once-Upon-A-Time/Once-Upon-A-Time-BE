package pproject.once_upon_a_time.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoSignupRequestDto {
    private String kakaoUserId;
    private String name;
    private String gender;
    private String birth; // "YYYY.MM.DD"
    private String nickname;
    private String personalPhone;
}
