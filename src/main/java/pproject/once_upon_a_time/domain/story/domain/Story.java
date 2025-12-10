package pproject.once_upon_a_time.domain.story.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stories")
public class Story extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id")
    private Long id;

    // [메타데이터 그룹]
    @Column(name = "project_name")
    private String projectName;

    @Column(length = 50)
    private String version;

    @Column(length = 50)
    private String modelType;

    private Integer totalSegments;

    // [스토리 정보 그룹]
    private String title;

    @Column(length = 100)
    private String theme;

    @Column(length = 100)
    private String vibe;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String originalPrompt;

    @Column(length = 50)
    private String targetAge;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

//    // [JSON 데이터 그룹]
//    @Convert(converter = KeywordsConverter.class)
//    @Column(columnDefinition = "json")
//    private List<String> keywords;
//
//    @Convert(converter = ScriptConverter.class)
//    @Column(columnDefinition = "json")
//    private List<ScriptItem> script;

    // [본문 및 미디어]
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    
    @Column(length = 500)
    private String thumbnailUrl;

    // [상태 및 로그]
//    @Enumerated(EnumType.STRING)
//    private GenerationStatus generationStatus;

    @Column(length = 50)
    private String verificationStatus;

    private LocalDateTime completedAt;
}
