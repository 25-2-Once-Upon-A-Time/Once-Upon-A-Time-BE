package pproject.once_upon_a_time.domain.character.domain;

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
@Table(name = "characters")
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id", nullable = false)
    private Long characterId; // 캐릭터 ID (BIGINT)

    @Column(name = "character_name", nullable = false, length = 50)
    private String characterName; // 캐릭터 이름

    @Column(name = "character_type", length = 30)
    private String characterType; // 캐릭터 타입

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 캐릭터 설명

    @Column(name = "voice_sample_url", nullable = false, length = 500)
    private String voiceSampleUrl; // 목소리 샘플 URL (5초 미리듣기)

    @Column(name = "voice_file_url", length = 500)
    private String voiceFileUrl; // TTS 완성 파일 URL

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 썸네일 이미지 URL

    @Column(name = "is_premium")
    private Boolean isPremium = false; // 프리미엄 여부 (기본 false)

    @Column(name = "display_order")
    private Integer displayOrder = 0; // 표시 순서 (기본 0)

    @Column(name = "is_active")
    private Boolean isActive = true; // 활성 여부 (기본 true)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일
}