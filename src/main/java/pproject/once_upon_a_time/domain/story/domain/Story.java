package pproject.once_upon_a_time.domain.story.domain;

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
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id", nullable = false)
    private Long storyId; // 고유 ID

    @Column(name = "title", length = 100)
    private String title; // 동화 제목

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 동화 줄거리 요약

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content; // 동화 본문

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status")
    private GenerationStatus generationStatus; // 생성 상태

    @Column(name = "tags", length = 200)
    private String tags; // 태그

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 썸네일 이미지 URL

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성 요청 시각

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 생성 완료 시각

    // ENUM 정의
    public enum GenerationStatus {
        pending,
        processing,
        completed,
        failed
    }
}