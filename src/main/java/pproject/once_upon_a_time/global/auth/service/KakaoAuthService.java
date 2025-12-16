package pproject.once_upon_a_time.global.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoTokenResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoUserInfoResponseDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final WebClient webClient;

    @Value("${kakao.client-id}")
    private String clientId;

    // ❌ 삭제: @Value("${kakao.redirect-uri}") private String redirectUri;

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com";
    private static final String KAKAO_API_URL = "https://kapi.kakao.com";

    // 파라미터로 받은 redirectUri 사용
    public String getKakaoLoginUrl(String redirectUri) {
        return KAKAO_AUTH_URL + "/oauth/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code";
    }

    // 파라미터로 받은 redirectUri 사용
    public KakaoTokenResponseDto getKakaoTokens(String code, String redirectUri) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri); // ★ 컨트롤러가 결정해준 주소 사용
        formData.add("code", code);

        return webClient.post()
            .uri(KAKAO_AUTH_URL + "/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(formData)
            .retrieve()
            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                clientResponse -> clientResponse.bodyToMono(String.class).map(body -> new CustomException(ErrorCode.KAKAO_TOKEN_ERROR, "카카오 토큰 발급 실패: " + body)))
            .bodyToMono(KakaoTokenResponseDto.class)
            .block();
    }

    public KakaoUserInfoResponseDto getKakaoUserInfo(String accessToken) {
        return webClient.get()
            .uri(KAKAO_API_URL + "/v2/user/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                clientResponse -> clientResponse.bodyToMono(String.class).map(body -> new CustomException(ErrorCode.KAKAO_USER_INFO_ERROR, "카카오 유저 정보 조회 실패: " + body)))
            .bodyToMono(KakaoUserInfoResponseDto.class)
            .block();
    }
}
