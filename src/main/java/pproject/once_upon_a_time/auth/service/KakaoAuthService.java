package pproject.once_upon_a_time.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import pproject.once_upon_a_time.auth.dto.response.KakaoTokenResponseDto;
import pproject.once_upon_a_time.auth.dto.response.KakaoUserInfoResponseDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final WebClient webClient;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com";
    private static final String KAKAO_API_URL = "https://kapi.kakao.com";

    public String getKakaoLoginUrl() {
        return KAKAO_AUTH_URL + "/oauth/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code";
    }

    public KakaoTokenResponseDto getKakaoTokens(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
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
