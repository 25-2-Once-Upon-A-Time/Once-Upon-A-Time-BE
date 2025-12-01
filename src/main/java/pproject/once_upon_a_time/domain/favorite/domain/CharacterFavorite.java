package pproject.once_upon_a_time.domain.favorite.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "character_favorites")
public class CharacterFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_favorite_id", nullable = false)
    private Long characterFavoriteId; // 캐릭터 찜 ID (PK)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 찜 생성일

    @Column(name = "character_id", nullable = false)
    private Long characterId; // 캐릭터 ID (FK)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 사용자 ID (FK)
}