package pproject.once_upon_a_time.global.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.global.auth.dto.request.SignupRequestDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoLoginResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoRedirectUrlResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.TokenResponseDto;
import pproject.once_upon_a_time.global.auth.service.AuthService;
import pproject.once_upon_a_time.global.auth.service.KakaoAuthService;
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
    public ResponseEntity<ApiResult<TokenResponseDto>> signup(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody SignupRequestDto requestDto
    ) {
        String signupToken = authorizationHeader.substring(7); // "Bearer " 제거
        TokenResponseDto responseDto = authService.signup(signupToken, requestDto);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
