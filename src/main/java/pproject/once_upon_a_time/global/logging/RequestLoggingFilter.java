package pproject.once_upon_a_time.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 공용 요청 로거: URI/메서드, 쿼리, 인증 헤더 존재 여부를 남긴다.
 * (본문이나 민감 정보는 기록하지 않음)
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> EXCLUDE_PATH_PREFIXES = Set.of(
            "/swagger",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return EXCLUDE_PATH_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String authHeader = request.getHeader("Authorization");
        String authHint = authHeader == null
                ? "none"
                : (authHeader.startsWith("Bearer ") ? "bearer" : "present");

        log.info("REQ {} {}{} auth={}", method, uri, query == null ? "" : "?" + query, authHint);

        filterChain.doFilter(request, response);
    }
}
