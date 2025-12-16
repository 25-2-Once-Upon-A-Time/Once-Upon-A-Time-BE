package pproject.once_upon_a_time.global.auth.controller;

import jakarta.servlet.http.HttpServletRequest; // 필수 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final AuthService authService;

    // 환경별 리다이렉트 주소 설정
    private static final String PROD_REDIRECT_URI = "https://once-upon-a-time-beta.vercel.app/kakao/callback";
    private static final String DEV_REDIRECT_URI = "http://localhost:3000/kakao/callback";

    @GetMapping("/kakao/url")
    public ResponseEntity<ApiResult<KakaoRedirectUrlResponseDto>> getKakaoLoginUrl(HttpServletRequest request) {
        String redirectUri = determineRedirectUri(request); // 주소 자동 결정
        String url = kakaoAuthService.getKakaoLoginUrl(redirectUri);
        return ResponseEntity.ok(ApiResult.ok(new KakaoRedirectUrlResponseDto(url)));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResult<KakaoLoginResponseDto>> kakaoLogin(
        @RequestParam("code") String code,
        HttpServletRequest request
    ) {
        String redirectUri = determineRedirectUri(request); // 주소 자동 결정
        log.info("Redirect URI 감지: {}", redirectUri);

        KakaoLoginResponseDto responseDto = authService.kakaoLogin(code, redirectUri);
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }

    // [핵심] 요청이 어디서 왔는지(Referer) 확인해서 주소를 결정하는 메서드
    private String determineRedirectUri(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("localhost")) {
            return DEV_REDIRECT_URI;
        }
        return PROD_REDIRECT_URI;
    }

    // ... (회원가입, 재발급, 로그아웃, DevLogin 등 나머지 메서드는 기존 유지) ...
    @PostMapping("/signup/kakao")
    public ResponseEntity<ApiResult<TokenResponseDto>> signup(@RequestHeader("Authorization") String authorizationHeader, @RequestBody SignupRequestDto requestDto) {
        String signupToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(ApiResult.ok(authService.signup(signupToken, requestDto)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResult<TokenResponseDto>> reissue(@RequestBody TokenReissueRequestDto request) {
        return ResponseEntity.ok(ApiResult.ok(authService.reissue(request)));
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
