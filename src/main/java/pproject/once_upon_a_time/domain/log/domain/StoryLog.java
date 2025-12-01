package pproject.once_upon_a_time.domain.log.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "story_log")
public class StoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storylog_id", nullable = false)
    private Long storyLogId; // 고유 식별자

    @Column(name = "date")
    private LocalDateTime date; // 통계 기준일

    @Column(name = "total_plays")
    private Integer totalPlays; // 해당 동화의 재생 횟수

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 마지막 업데이트 시각

    @Column(name = "story_id", nullable = false)
    private Long storyId; // 동화 ID (FK)

    @Column(name = "playback_id", nullable = false)
    private Long playbackId; // 재생 ID (FK)
}