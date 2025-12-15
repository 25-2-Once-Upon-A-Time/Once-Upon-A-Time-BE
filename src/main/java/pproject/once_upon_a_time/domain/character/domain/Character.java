package pproject.once_upon_a_time.domain.character.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;
import pproject.once_upon_a_time.global.common.StringListConverter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "characters")
public class Character extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long id;

    @Column(name = "character_name", nullable = false, length = 50)
    private String characterName;

    @Column(name = "character_type", length = 30)
    private String characterType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 500)
    private String voiceSampleUrl; // 기존 필드 유지

    @Column(length = 100)
    private String voiceActor;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    @Column(length = 500)
    private String modelPath;

    @Column(length = 50)
    private String speakerId;

    // [NEW] 샘플 오디오 리스트 추가 (CharacterSampleAudio 클래스는 별도 파일로 정의 필요)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "character_sample_audios",
        joinColumns = @JoinColumn(name = "character_id"))
    private List<CharacterSampleAudio> sampleAudios = new ArrayList<>();

}
