package pproject.once_upon_a_time.domain.token.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.member.domain.Member;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 512)
    private String token;

    public void updateToken(String token) {
        this.token = token;
    }
}