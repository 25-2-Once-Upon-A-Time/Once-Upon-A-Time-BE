package pproject.once_upon_a_time.domain.log.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "character_log")
public class CharacterLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "characterlog_id", nullable = false)
    private Long characterLogId; // 고유 식별자

    @Column(name = "date")
    private LocalDateTime date; // 통계 기준일 (예: 2025-11-08)

    @Column(name = "total_plays")
    private Integer totalPlays; // 해당 캐릭터의 재생 횟수

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 통계가 마지막 업데이트된 시각

    @Column(name = "character_id", nullable = false)
    private Long characterId; // 통계를 집계할 캐릭터 ID (FK)

    @Column(name = "playback_id", nullable = false)
    private Long playbackId; // 재생 ID (FK)
}