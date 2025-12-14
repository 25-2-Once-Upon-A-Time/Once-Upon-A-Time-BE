package pproject.once_upon_a_time.domain.story.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;
import pproject.once_upon_a_time.global.common.StringListConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // [삭제됨] projectName (고정값이므로 삭제)

    // [유지] 모델 버전 관리를 위해 남겨둠 (필요 없으면 삭제 가능)
    private String version;
    private String modelType;
    private Integer totalSegments;

    // --- 사용자 요청 정보 ---
    private String title;
    private String theme;
    private String vibe;

    @Lob
    private String originalPrompt;

    // --- AI 생성 정보 ---
    private String targetAge;

    @Lob
    private String summary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private GenerationStatus generationStatus;

    // [삭제됨] verificationStatus (generationStatus로 충분함)

    @Column(length = 500)
    private String thumbnailUrl;

    private LocalDateTime completedAt;

    // [수정] json 타입 강제 제거 -> 호환성 확보
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Script> scripts = new ArrayList<>();

    @Builder
    public Story(Member member, String version, String modelType, Integer totalSegments,
        String title, String theme, String vibe, String originalPrompt,
        String targetAge, String summary, String content,
        GenerationStatus generationStatus, String thumbnailUrl,
        LocalDateTime completedAt, List<String> keywords) {
        this.member = member;
        this.version = version;
        this.modelType = modelType;
        this.totalSegments = totalSegments;
        this.title = title;
        this.theme = theme;
        this.vibe = vibe;
        this.originalPrompt = originalPrompt;
        this.targetAge = targetAge;
        this.summary = summary;
        this.content = content;
        this.generationStatus = generationStatus;
        this.thumbnailUrl = thumbnailUrl;
        this.completedAt = completedAt;
        this.keywords = keywords;
    }

    // 메서드 파라미터에서도 projectName 제거
    public void updateWithAiResponse(String content, String summary, List<String> keywords,
        String version, String modelType, Integer totalSegments) {
        this.content = content;
        this.summary = summary;
        this.keywords = keywords;
        this.version = version;
        this.modelType = modelType;
        this.totalSegments = totalSegments;
    }

    public void completeGeneration() {
        this.generationStatus = GenerationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now(); // 완료 시간 자동 기록 추천
    }

    public void failGeneration() {
        this.generationStatus = GenerationStatus.FAILED;
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
