package pproject.once_upon_a_time.domain.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.token.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
