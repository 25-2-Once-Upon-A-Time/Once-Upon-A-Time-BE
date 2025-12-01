package pproject.once_upon_a_time.domain.audiobook.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audio_books")
public class AudioBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audiobook_id", nullable = false)
    private Long audiobookId; // 고유 식별자

    @Column(name = "audio_url", length = 255)
    private String audioUrl; // WAV 파일 경로

    @Column(name = "story_id", nullable = false)
    private Long storyId; // 원본 동화 ID (FK)

    @Column(name = "character_id", nullable = false)
    private Long characterId; // 캐릭터 ID (FK)
}