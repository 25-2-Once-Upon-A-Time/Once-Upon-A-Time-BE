package pproject.once_upon_a_time.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.auth.dto.request.KakaoSignupRequestDto;
import pproject.once_upon_a_time.auth.dto.response.KakaoLoginResponseDto;
import pproject.once_upon_a_time.auth.dto.response.KakaoRedirectUrlResponseDto;
import pproject.once_upon_a_time.auth.dto.response.TokenResponseDto;
import pproject.once_upon_a_time.auth.service.AuthService;
import pproject.once_upon_a_time.auth.service.KakaoAuthService;
import pproject.once_upon_a_time.global.response.ApiResult;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final AuthService authService;

    @GetMapping("/kakao/url")
    public ResponseEntity<ApiResult<KakaoRedirectUrlResponseDto>> getKakaoLoginUrl() {
        String url = kakaoAuthService.getKakaoLoginUrl();
        KakaoRedirectUrlResponseDto responseDto = new KakaoRedirectUrlResponseDto(url);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResult<KakaoLoginResponseDto>> kakaoLogin(@RequestParam("code") String code) {
        KakaoLoginResponseDto responseDto = authService.kakaoLogin(code);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }

    @PostMapping("/signup/kakao")
    public ResponseEntity<ApiResult<TokenResponseDto>> signup(@RequestBody KakaoSignupRequestDto requestDto) {
        TokenResponseDto responseDto = authService.signup(requestDto);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
