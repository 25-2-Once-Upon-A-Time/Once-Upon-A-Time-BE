package pproject.once_upon_a_time.domain.scripts.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "scripts",
        indexes = {
                @Index(name = "idx_scripts_story_seq", columnList = "story_id, seq"),
                @Index(name = "idx_scripts_role", columnList = "role")
        }
)
public class Scripts extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id")
    private Long id;

    // Story FK 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    // 순서 (1, 2, 3 …)
    @Column(nullable = false)
    private Integer seq;

    // narrator / princess / prince 등
    @Column(length = 50, nullable = false)
    private String role;

    // 텍스트
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    // 감정값 (neutral, happy, sad …)
    @Column(length = 50)
    private String emotion;

    // 오디오 파일 정보
    @Column(length = 255)
    private String audioFileName;

    @Column(length = 500)
    private String audioFilePath;

    private Float duration; // 오디오 길이(초)

}
