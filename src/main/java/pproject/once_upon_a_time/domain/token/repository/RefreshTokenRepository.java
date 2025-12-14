package pproject.once_upon_a_time.domain.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.token.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // [추가] 토큰 값으로 RefreshToken 엔티티 찾기 (재발급 시 사용)
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}