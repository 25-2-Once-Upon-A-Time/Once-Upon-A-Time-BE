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
@Table(name = "story_favorites")
public class StoryFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_favorite_id", nullable = false)
    private Long storyFavoriteId; // 고유 ID

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 찜한 날짜

    @Column(name = "story_id", nullable = false)
    private Long storyId; // 찜한 동화 ID (FK)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 사용자 ID (FK)
}