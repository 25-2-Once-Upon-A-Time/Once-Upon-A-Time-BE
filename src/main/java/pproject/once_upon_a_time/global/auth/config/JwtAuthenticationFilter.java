package pproject.once_upon_a_time.global.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private static final List<String> EXCLUDE_URLS = Arrays.asList(
        "/api/v1/auth/signup/kakao",    // 회원가입 (signupToken 사용 -> 권한 없음 -> 필터 제외 필수)
        "/api/v1/auth/kakao/callback",  // 카카오 콜백 (인증 전)
        "/api/v1/auth/kakao/url"        // 로그인 URL 요청 (인증 전)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 2. resolveToken으로 토큰 꺼내기
        String token = jwtProvider.resolveToken(request);

        // 3. 토큰이 있고 유효하면 인증 처리 (shouldNotFilter 덕분에 회원가입은 여기 안 옴)
        if (token != null && jwtProvider.validateToken(token)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return EXCLUDE_URLS.stream().anyMatch(url -> request.getRequestURI().startsWith(url));
    }
}
