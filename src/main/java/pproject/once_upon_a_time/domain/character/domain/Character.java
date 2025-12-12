package pproject.once_upon_a_time.domain.character.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;
import pproject.once_upon_a_time.global.common.StringListConverter;

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
    
    // --- 신규 추가 필드 ---
    @Column(length = 100)
    private String voiceActor;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> tags;

    @Column(length = 500)
    private String modelPath;

    @Column(length = 50)
    private String speakerId;
}