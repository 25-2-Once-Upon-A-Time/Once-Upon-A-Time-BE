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

    private String projectName;
    private String version;
    private String modelType;
    private Integer totalSegments;

    private String title;
    private String theme;
    private String vibe;

    @Lob
    private String originalPrompt;

    private String targetAge; // AI응답 필드에 없지만, 초기 요청에 있을 수 있으므로 유지.

    @Lob
    private String summary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private GenerationStatus generationStatus;

    @Column(length = 500)
    private String thumbnailUrl; // 로컬 저장 경로 (AI 응답에 없으므로 별도 처리 필요)

    private LocalDateTime completedAt;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Script> scripts = new ArrayList<>();

    @Builder
    public Story(Member member, String projectName, String version, String modelType, Integer totalSegments, String title, String theme, String vibe, String originalPrompt, String targetAge, String summary, String content, GenerationStatus generationStatus, String thumbnailUrl, LocalDateTime completedAt, List<String> keywords) {
        this.member = member;
        this.projectName = projectName;
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

    // AI 응답으로 엔티티를 업데이트하기 위한 메서드
    public void updateWithAiResponse(String content, String summary, List<String> keywords, String projectName, String version, String modelType, Integer totalSegments) {
        this.content = content;
        this.summary = summary;
        this.keywords = keywords;
        this.projectName = projectName;
        this.version = version;
        this.modelType = modelType;
        this.totalSegments = totalSegments;
    }
    
    public void completeGeneration() {
        this.generationStatus = GenerationStatus.COMPLETED;
    }

    public void failGeneration() {
        this.generationStatus = GenerationStatus.FAILED;
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
