package pproject.once_upon_a_time.global.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값인 필드는 응답에 포함되지 않음
public class KakaoLoginResponseDto {
    // 공통 필드
    private Boolean isNewUser;

    // 신규 유저일 경우, 회원가입용 임시 토큰
    private String signupToken;

    // 기존 유저일 경우, 서비스 토큰
    private String accessToken;
    private String refreshToken;
}