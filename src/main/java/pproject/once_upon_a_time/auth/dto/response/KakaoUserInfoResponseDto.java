package pproject.once_upon_a_time.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponseDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private Profile profile;
        private String email;
        // 추가 정보 동의 시 받을 수 있는 필드들...
    }

    @Getter
    @NoArgsConstructor
    public static class Profile {
        private String nickname;
        @JsonProperty("profile_image_url")
        private String profileImageUrl;
        // ...
    }
}
