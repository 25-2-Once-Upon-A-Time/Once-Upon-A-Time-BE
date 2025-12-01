package pproject.once_upon_a_time.domain.token.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "account_token")
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false)
    private Long tokenId; // FCM 토큰 ID (PK)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 회원 아이디 (FK)

    @Column(name = "ip_address", length = 200)
    private String ipAddress; // 보안 고려용 IP

    @Column(name = "user_agent", length = 200)
    private String userAgent; // 기기 / 브라우저

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 만료일

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일
}