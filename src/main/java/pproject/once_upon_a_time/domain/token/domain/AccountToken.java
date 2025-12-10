package pproject.once_upon_a_time.domain.token.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "account_token")
public class AccountToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member memberId;

    @Column(length = 200)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    private LocalDateTime expiresAt;

}
