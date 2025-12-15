package pproject.once_upon_a_time.global.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenReissueRequestDto {
    private String accessToken;
    private String refreshToken;
}