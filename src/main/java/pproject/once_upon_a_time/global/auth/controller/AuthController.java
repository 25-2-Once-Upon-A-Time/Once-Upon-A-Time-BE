package pproject.once_upon_a_time.global.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.global.auth.dto.request.SignupRequestDto;
import pproject.once_upon_a_time.global.auth.dto.request.TokenReissueRequestDto;
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

    // [수정] 프론트엔드로부터 redirectUri를 받아서 처리 (로그인 창 주소 생성)
    @GetMapping("/kakao/url")
    public ResponseEntity<ApiResult<KakaoRedirectUrlResponseDto>> getKakaoLoginUrl(
        @RequestParam("redirectUri") String redirectUri
    ) {
        // 서비스에 주소 전달
        String url = kakaoAuthService.getKakaoLoginUrl(redirectUri);
        KakaoRedirectUrlResponseDto responseDto = new KakaoRedirectUrlResponseDto(url);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }

    // [수정] 프론트엔드로부터 code와 redirectUri를 둘 다 받음 (토큰 교환)
    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResult<KakaoLoginResponseDto>> kakaoLogin(
        @RequestParam("code") String code,
        @RequestParam("redirectUri") String redirectUri
    ) {
        // 서비스에 주소 전달
        KakaoLoginResponseDto responseDto = authService.kakaoLogin(code, redirectUri);
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

    @PostMapping("/reissue")
    public ResponseEntity<ApiResult<TokenResponseDto>> reissue(@RequestBody TokenReissueRequestDto request) {
        TokenResponseDto responseDto = authService.reissue(request);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @GetMapping("/dev/login")
    public ResponseEntity<ApiResult<TokenResponseDto>> devLogin(@RequestParam Long memberId) {
        return ResponseEntity.ok(ApiResult.ok(authService.devLogin(memberId)));
    }
}
