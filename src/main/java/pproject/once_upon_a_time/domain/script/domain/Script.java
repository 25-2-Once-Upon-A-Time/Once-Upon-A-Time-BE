package pproject.once_upon_a_time.domain.script.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.story.domain.Story;

@Entity
@Table(name = "scripts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(nullable = false)
    private Integer seq; // 대본 순서

    @Column(length = 100)
    private String role; // 역할

    @Lob
    @Column(columnDefinition = "TEXT")
    private String text; // 대사

    @Column(length = 50)
    private String emotion; // [추가됨] AI가 감정도 분석해주니까 저장해야 함!

    @Column(length = 500)
    private String audioFilePath;

    @Builder
    public Script(Story story, Integer seq, String role, String text, String emotion, String audioFilePath) {
        this.story = story;
        this.seq = seq;
        this.role = role;
        this.text = text;
        this.emotion = emotion; // [추가됨]
        this.audioFilePath = audioFilePath;
    }
}
