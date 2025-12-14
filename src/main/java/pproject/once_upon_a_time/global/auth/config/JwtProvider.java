package pproject.once_upon_a_time.global.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import pproject.once_upon_a_time.global.auth.dto.response.TokenResponseDto;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.common.MemberRole; // Role Enum import 확인

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;

    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7; // 7일
    private static final long SIGNUP_TOKEN_EXPIRE_TIME = 1000 * 60 * 10; // 10분

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // [추가] Access Token 만료 시간 조회 (AuthService에서 사용)
    public Long getAccessTokenExpirationTime() {
        return ACCESS_TOKEN_EXPIRE_TIME;
    }

    public String createSignupToken(String kakaoUserId) {
        Claims claims = Jwts.claims().setSubject(kakaoUserId);
        claims.put("type", "signup");

        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + SIGNUP_TOKEN_EXPIRE_TIME))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String getKakaoUserIdFromSignupToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!"signup".equals(claims.get("type"))) {
                return null;
            }
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // [수정] DTO 변경에 맞춰 Builder 패턴으로 변경
    public TokenResponseDto createToken(Member member) {
        String accessToken = createAccessToken(member.getId(), member.getRole());

        Date now = new Date();
        String refreshToken = Jwts.builder()
            .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        return TokenResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    // [추가] Access Token만 따로 만드는 메서드 (재발급용)
    public String createAccessToken(Long memberId, MemberRole role) {
        Claims claims = Jwts.claims().setSubject(memberId.toString());
        claims.put("role", role.toString());

        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("role") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(claims.get("role").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public Long getMemberIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
