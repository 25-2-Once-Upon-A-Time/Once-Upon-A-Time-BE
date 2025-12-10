package pproject.once_upon_a_time.domain.character.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

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

    private Integer displayOrder;

    private Boolean isActive;

    private Boolean isPremium;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String voiceFileUrl;

    @Column(nullable = false, length = 500)
    private String voiceSampleUrl;
}